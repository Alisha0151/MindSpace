package com.mindspace.habit;

import com.mindspace.common.ConflictException;
import com.mindspace.common.DateRange;
import com.mindspace.common.DateRules;
import com.mindspace.common.FieldViolation;
import com.mindspace.common.NotFoundException;
import com.mindspace.common.Text;
import com.mindspace.common.ValidationException;
import com.mindspace.habit.HabitCompletionRepository.HabitDate;
import com.mindspace.habit.HabitDtos.HabitProgress;
import com.mindspace.habit.HabitDtos.HabitRequest;
import com.mindspace.habit.HabitDtos.HabitResponse;
import com.mindspace.habit.domain.Cadence;
import com.mindspace.habit.domain.HabitDefinition;
import com.mindspace.habit.domain.HabitRules;
import com.mindspace.habit.domain.PauseWindow;
import com.mindspace.habit.domain.StreakCalculator;
import com.mindspace.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Streak and progress maths live in the framework-free {@link StreakCalculator}; this class loads data and enforces ownership. */
@Service
public class HabitService {

    static final int MAX_HABITS_PER_USER = 50;
    static final int MAX_PROGRESS_DAYS = 366;

    private final HabitRepository habits;
    private final HabitPauseRepository pauses;
    private final HabitCompletionRepository completions;
    private final UserService users;
    private final Clock clock;

    public HabitService(HabitRepository habits, HabitPauseRepository pauses, HabitCompletionRepository completions,
                        UserService users, Clock clock) {
        this.habits = habits;
        this.pauses = pauses;
        this.completions = completions;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<HabitResponse> list(UUID userId) {
        LocalDate today = today(userId);
        List<Habit> mine = habits.findByUserIdOrderByCreatedAtAsc(userId);
        if (mine.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = mine.stream().map(Habit::getId).toList();
        Map<UUID, Set<LocalDate>> datesByHabit = new HashMap<>();
        for (HabitDate d : completions.findDates(userId, ids)) {
            datesByHabit.computeIfAbsent(d.getHabitId(), k -> new HashSet<>()).add(d.getCompletedOn());
        }
        Map<UUID, List<PauseWindow>> pausesByHabit = new HashMap<>();
        for (HabitPause p : pauses.findByUserId(userId)) {
            pausesByHabit.computeIfAbsent(p.getHabitId(), k -> new ArrayList<>()).add(new PauseWindow(p.getPausedOn(), p.getResumedOn()));
        }
        return mine.stream()
                .map(h -> toResponse(h, definition(h, pausesByHabit.getOrDefault(h.getId(), List.of())),
                        datesByHabit.getOrDefault(h.getId(), Set.of()), today))
                .toList();
    }

    @Transactional(readOnly = true)
    public HabitResponse get(UUID userId, UUID habitId) {
        Habit habit = owned(userId, habitId);
        return toResponse(habit, definitionFor(habit), completionDates(userId, habitId), today(userId));
    }

    @Transactional
    public HabitResponse create(UUID userId, HabitRequest req) {
        if (habits.countByUserId(userId) >= MAX_HABITS_PER_USER) {
            throw new ConflictException("You can track up to " + MAX_HABITS_PER_USER + " habits");
        }
        Habit habit = new Habit(userId);
        applyRequest(habit, req, today(userId));
        Habit saved = habits.saveAndFlush(habit);   // name uniqueness is enforced by habits_user_name_uq
        return toResponse(saved, definitionFor(saved), Set.of(), today(userId));
    }

    @Transactional
    public HabitResponse update(UUID userId, UUID habitId, HabitRequest req) {
        Habit habit = owned(userId, habitId);
        applyRequest(habit, req, today(userId));
        Habit saved = habits.saveAndFlush(habit);
        return toResponse(saved, definitionFor(saved), completionDates(userId, habitId), today(userId));
    }

    @Transactional
    public void delete(UUID userId, UUID habitId) {
        if (habits.deleteByIdAndUserId(habitId, userId) == 0) {
            throw new NotFoundException("Habit");
        }
    }

    @Transactional
    public HabitResponse pause(UUID userId, UUID habitId) {
        Habit habit = owned(userId, habitId);
        if (pauses.findFirstByHabitIdAndUserIdAndResumedOnIsNull(habitId, userId).isPresent()) {
            throw new ConflictException("This habit is already paused");
        }
        pauses.saveAndFlush(new HabitPause(habitId, userId, today(userId)));
        return toResponse(habit, definitionFor(habit), completionDates(userId, habitId), today(userId));
    }

    @Transactional
    public HabitResponse resume(UUID userId, UUID habitId) {
        Habit habit = owned(userId, habitId);
        LocalDate today = today(userId);
        HabitPause open = pauses.findFirstByHabitIdAndUserIdAndResumedOnIsNull(habitId, userId)
                .orElseThrow(() -> new ConflictException("This habit is not paused"));
        if (open.getPausedOn().isBefore(today)) {
            open.setResumedOn(today);
            pauses.saveAndFlush(open);
        } else {
            // Paused and resumed on the same day: a pause window needs at least one day, so simply undo it.
            pauses.delete(open);
            pauses.flush();
        }
        return toResponse(habit, definitionFor(habit), completionDates(userId, habitId), today);
    }

    /** Idempotent: completing an already-completed day succeeds and changes nothing. */
    @Transactional
    public void complete(UUID userId, UUID habitId, LocalDate day) {
        Habit habit = owned(userId, habitId);
        ValidationException.throwIfAny(HabitRules.completionViolations(definitionFor(habit), day, today(userId)));
        completions.insertIfAbsent(userId, habitId, day);
    }

    /** Idempotent "undo": removing a completion that does not exist is not an error. */
    @Transactional
    public void uncomplete(UUID userId, UUID habitId, LocalDate day) {
        owned(userId, habitId);
        completions.deleteCompletion(userId, habitId, day);
    }

    @Transactional(readOnly = true)
    public HabitProgress progress(UUID userId, UUID habitId, LocalDate from, LocalDate to) {
        Habit habit = owned(userId, habitId);
        LocalDate today = today(userId);
        DateRange range = DateRange.resolve(from, to, today, 28, MAX_PROGRESS_DAYS);
        HabitDefinition def = definitionFor(habit);
        Set<LocalDate> all = completionDates(userId, habitId);
        List<LocalDate> inRange = completions.findDatesInRange(userId, habitId, range.from(), range.to());
        return new HabitProgress(habitId, range.from(), range.to(), inRange,
                StreakCalculator.weeklyProgress(all, def, range.from(), range.to()),
                StreakCalculator.consistency(all, def, range.from(), range.to()),
                StreakCalculator.currentStreak(all, def, today),
                StreakCalculator.longestStreak(all, def),
                StreakCalculator.weeklyStreak(all, def, today));
    }

    private void applyRequest(Habit habit, HabitRequest req, LocalDate today) {
        List<FieldViolation> problems = new ArrayList<>();
        if (Text.isBlank(req.name())) {
            problems.add(new FieldViolation("name", "must not be blank"));
        }
        int target = req.targetCount() != null ? req.targetCount() : 1;
        LocalDate start = req.startDate() != null ? req.startDate() : today;
        try {
            new HabitDefinition(req.cadence(), target, start, req.endDate(), List.of());
        } catch (IllegalArgumentException e) {
            problems.add(new FieldViolation("targetCount", e.getMessage()));
        }
        ValidationException.throwIfAny(problems);
        habit.apply(Text.trim(req.name()), Text.isBlank(req.description()) ? null : Text.trim(req.description()),
                req.cadence(), target, start, req.endDate());
    }

    private Habit owned(UUID userId, UUID habitId) {
        return habits.findByIdAndUserId(habitId, userId).orElseThrow(() -> new NotFoundException("Habit"));
    }

    private LocalDate today(UUID userId) {
        return DateRules.localDate(clock.instant(), users.zoneOf(userId));
    }

    private Set<LocalDate> completionDates(UUID userId, UUID habitId) {
        Set<LocalDate> dates = new HashSet<>();
        for (HabitDate d : completions.findDates(userId, List.of(habitId))) {
            dates.add(d.getCompletedOn());
        }
        return dates;
    }

    private HabitDefinition definitionFor(Habit habit) {
        List<PauseWindow> windows = pauses.findByHabitIdAndUserId(habit.getId(), habit.getUserId()).stream()
                .map(p -> new PauseWindow(p.getPausedOn(), p.getResumedOn())).toList();
        return definition(habit, windows);
    }

    private static HabitDefinition definition(Habit h, List<PauseWindow> windows) {
        return new HabitDefinition(h.getCadence(), h.getTargetCount(), h.getStartDate(), h.getEndDate(), windows);
    }

    static HabitResponse toResponse(Habit h, HabitDefinition def, Set<LocalDate> done, LocalDate today) {
        LocalDate openPauseSince = def.pauses().stream().filter(p -> p.resumedOn() == null)
                .map(PauseWindow::pausedOn).findFirst().orElse(null);
        List<Boolean> last7 = new ArrayList<>(7);
        for (int i = 6; i >= 0; i--) {
            last7.add(done.contains(today.minusDays(i)));
        }
        LocalDate weekStart = DateRules.weekStart(today);
        int thisWeek = (int) done.stream().filter(d -> DateRules.inRange(d, weekStart, weekStart.plusDays(6))).count();
        return new HabitDtos.HabitResponse(h.getId(), h.getName(), h.getDescription(), h.getCadence(), h.getTargetCount(),
                h.getStartDate(), h.getEndDate(), openPauseSince != null, openPauseSince,
                done.contains(today),
                StreakCalculator.currentStreak(done, def, today),
                StreakCalculator.longestStreak(done, def),
                h.getCadence() == Cadence.WEEKLY ? StreakCalculator.weeklyStreak(done, def, today) : 0,
                thisWeek, last7,
                StreakCalculator.consistency(done, def, today.minusDays(29), today));
    }
}
