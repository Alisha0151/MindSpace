package com.mindspace.analytics;

import com.mindspace.analytics.AnalyticsDtos.AnalyticsSummary;
import com.mindspace.analytics.AnalyticsDtos.DailyPoint;
import com.mindspace.analytics.domain.CheckInPoint;
import com.mindspace.analytics.domain.MoodAnalytics;
import com.mindspace.checkin.CheckInRepository;
import com.mindspace.checkin.CheckInTag;
import com.mindspace.checkin.MoodCheckIn;
import com.mindspace.checkin.TagType;
import com.mindspace.common.DateRange;
import com.mindspace.common.DateRules;
import com.mindspace.common.Numbers;
import com.mindspace.common.NotFoundException;
import com.mindspace.common.ValidationException;
import com.mindspace.habit.HabitCompletionRepository;
import com.mindspace.habit.HabitRepository;
import com.mindspace.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Loads at most 366 days of the user's own check-ins and delegates all arithmetic to {@link MoodAnalytics}. */
@Service
public class AnalyticsService {

    static final int MAX_DAYS = 366;

    private final CheckInRepository checkIns;
    private final HabitRepository habits;
    private final HabitCompletionRepository completions;
    private final UserService users;
    private final Clock clock;

    public AnalyticsService(CheckInRepository checkIns, HabitRepository habits, HabitCompletionRepository completions,
                            UserService users, Clock clock) {
        this.checkIns = checkIns;
        this.habits = habits;
        this.completions = completions;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AnalyticsSummary summary(UUID userId, LocalDate from, LocalDate to,
                                    LocalDate compareFrom, LocalDate compareTo, UUID habitId) {
        LocalDate today = DateRules.localDate(clock.instant(), users.zoneOf(userId));
        DateRange range = DateRange.resolve(from, to, today, 30, MAX_DAYS);
        if ((compareFrom == null) != (compareTo == null)) {
            throw new ValidationException("compareFrom", "compareFrom and compareTo must be given together");
        }
        DateRange compare = null;
        if (compareFrom != null) {
            compare = DateRange.resolve(compareFrom, compareTo, today, 30, MAX_DAYS);
        }
        if (habitId != null && habits.findByIdAndUserId(habitId, userId).isEmpty()) {
            throw new NotFoundException("Habit");
        }

        LocalDate loadFrom = compare == null || range.from().isBefore(compare.from()) ? range.from() : compare.from();
        LocalDate loadTo = compare == null || range.to().isAfter(compare.to()) ? range.to() : compare.to();
        List<MoodCheckIn> loaded = checkIns.findByUserIdAndLocalDateBetweenOrderByLocalDateAsc(userId, loadFrom, loadTo);
        List<CheckInPoint> all = loaded.stream().map(AnalyticsService::toPoint).toList();
        List<CheckInPoint> inRange = all.stream()
                .filter(p -> DateRules.inRange(p.date(), range.from(), range.to())).toList();

        BigDecimal avg = inRange.isEmpty() ? null : Numbers.average(inRange.stream().mapToLong(CheckInPoint::mood).sum(), inRange.size(), 2);
        List<DailyPoint> daily = inRange.stream()
                .map(p -> new DailyPoint(p.date(), p.mood(), p.stress(), p.energy(), p.sleepMinutes())).toList();

        MoodAnalytics.HabitMoodComparison habitComparison = null;
        if (habitId != null) {
            Set<LocalDate> done = new HashSet<>(completions.findDatesInRange(userId, habitId, range.from(), range.to()));
            habitComparison = MoodAnalytics.habitVersusMood(inRange, done);
        }
        MoodAnalytics.RangeComparison comparison = compare == null ? null
                : MoodAnalytics.compareRanges(all, range.from(), range.to(), compare.from(), compare.to());

        return new AnalyticsSummary(range.from(), range.to(), inRange.size(), MoodAnalytics.hasEnoughData(inRange.size()),
                avg, daily, MoodAnalytics.weeklyTrend(inRange), MoodAnalytics.monthlyTrend(inRange),
                MoodAnalytics.moodDistribution(inRange), MoodAnalytics.topEmotions(inRange, 8),
                MoodAnalytics.sleepVersusMood(inRange), MoodAnalytics.coverage(inRange, range.from(), range.to()),
                habitId, habitComparison, comparison,
                compare == null ? null : compare.from(), compare == null ? null : compare.to());
    }

    static CheckInPoint toPoint(MoodCheckIn c) {
        Set<String> emotions = c.getTags().stream().filter(t -> t.getTagType() == TagType.EMOTION)
                .map(CheckInTag::getLabel).collect(Collectors.toSet());
        return new CheckInPoint(c.getLocalDate(), c.getMoodScore(), c.getStressScore(), c.getEnergyScore(),
                c.getSleepMinutes(), emotions);
    }
}
