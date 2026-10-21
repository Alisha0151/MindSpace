package com.mindspace.dashboard;

import com.mindspace.checkin.CheckInDtos.CheckInResponse;
import com.mindspace.checkin.CheckInRepository;
import com.mindspace.checkin.CheckInService;
import com.mindspace.checkin.MoodCheckIn;
import com.mindspace.common.DateRules;
import com.mindspace.dashboard.DashboardDtos.Dashboard;
import com.mindspace.dashboard.DashboardDtos.MoodDot;
import com.mindspace.habit.HabitDtos.HabitResponse;
import com.mindspace.habit.HabitService;
import com.mindspace.habit.domain.Cadence;
import com.mindspace.journal.JournalRepository;
import com.mindspace.journal.JournalService;
import com.mindspace.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DashboardService {

    private static final int STREAK_LOOKBACK_DAYS = 60;

    private final CheckInRepository checkIns;
    private final JournalRepository journal;
    private final HabitService habits;
    private final UserService users;
    private final Clock clock;

    public DashboardService(CheckInRepository checkIns, JournalRepository journal, HabitService habits,
                            UserService users, Clock clock) {
        this.checkIns = checkIns;
        this.journal = journal;
        this.habits = habits;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Dashboard load(UUID userId) {
        LocalDate today = DateRules.localDate(clock.instant(), users.zoneOf(userId));
        List<MoodCheckIn> recent = checkIns.findByUserIdAndLocalDateBetweenOrderByLocalDateAsc(
                userId, today.minusDays(STREAK_LOOKBACK_DAYS), today);
        Map<LocalDate, MoodCheckIn> byDate = new HashMap<>();
        for (MoodCheckIn c : recent) {
            byDate.put(c.getLocalDate(), c);
        }
        CheckInResponse todayCheckIn = byDate.containsKey(today) ? CheckInService.toResponse(byDate.get(today)) : null;

        // Check-in streak: consecutive logged days ending today, or yesterday if today is still open.
        LocalDate cursor = byDate.containsKey(today) ? today : today.minusDays(1);
        int streak = 0;
        while (byDate.containsKey(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }

        List<MoodDot> dots = new ArrayList<>(7);
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            MoodCheckIn c = byDate.get(d);
            dots.add(new MoodDot(d, c == null ? null : c.getMoodScore()));
        }

        List<HabitResponse> habitList = habits.list(userId);
        List<HabitResponse> active = habitList.stream().filter(h -> !h.paused()).toList();
        int doneToday = (int) active.stream().filter(HabitResponse::doneToday).count();
        // "Due today": every active daily habit, and weekly habits that have not yet met their target this week.
        int dueToday = (int) active.stream()
                .filter(h -> h.cadence() == Cadence.DAILY || h.thisWeekDone() < h.targetCount() || h.doneToday())
                .count();

        var recentJournal = journal.findTop5ByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(JournalService::toSummary).toList();

        return new Dashboard(users.profile(userId).displayName(), today, todayCheckIn, streak, dots, habitList,
                doneToday, dueToday, recentJournal);
    }
}
