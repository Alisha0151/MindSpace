package com.mindspace.dashboard;

import com.mindspace.checkin.CheckInDtos.CheckInResponse;
import com.mindspace.habit.HabitDtos.HabitResponse;
import com.mindspace.journal.JournalDtos.JournalSummary;

import java.time.LocalDate;
import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record MoodDot(LocalDate date, Integer mood) {
    }

    /** Aggregate for the home screen. Journal items are summaries (title, excerpt) the owner already sees in the journal list. */
    public record Dashboard(String displayName, LocalDate today, CheckInResponse todayCheckIn, int checkInStreak,
                            List<MoodDot> last7Days, List<HabitResponse> habits, int habitsDoneToday,
                            int habitsDueToday, List<JournalSummary> recentJournal) {
    }
}
