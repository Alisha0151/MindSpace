package com.mindspace.habit;

import com.mindspace.habit.domain.Cadence;
import com.mindspace.habit.domain.StreakCalculator.Consistency;
import com.mindspace.habit.domain.StreakCalculator.WeekProgress;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class HabitDtos {

    private HabitDtos() {
    }

    public record HabitRequest(@NotBlank @Size(max = 100) String name,
                               @Size(max = 500) String description,
                               @NotNull Cadence cadence,
                               Integer targetCount,
                               LocalDate startDate,
                               LocalDate endDate) {
    }

    public record HabitResponse(UUID id, String name, String description, Cadence cadence, int targetCount,
                                LocalDate startDate, LocalDate endDate, boolean paused, LocalDate pausedSince,
                                boolean doneToday, int currentStreak, int longestStreak, int weeklyStreak,
                                int thisWeekDone, List<Boolean> last7Days, Consistency consistency30) {
    }

    public record HabitProgress(UUID habitId, LocalDate from, LocalDate to, List<LocalDate> completions,
                                List<WeekProgress> weeks, Consistency consistency,
                                int currentStreak, int longestStreak, int weeklyStreak) {
    }
}
