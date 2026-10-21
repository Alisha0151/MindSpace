package com.mindspace.habit.domain;

import com.mindspace.common.DateRules;
import com.mindspace.common.Numbers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Streak, weekly-progress and consistency calculations. Every method is a pure function of the
 * completion dates and the habit definition; callers pass only that habit's completions.
 *
 * <p>Definitions:
 * <ul>
 *   <li><b>Daily streak ending on D</b>: consecutive completed days counting back from D.
 *       Days inside a pause window are skipped (they neither count nor break the streak).
 *       Zero if D itself is neither completed nor paused.</li>
 *   <li><b>Current streak</b>: the streak ending today, or ending yesterday if today is not done
 *       yet. Today stays open, so an unfinished day never resets the streak.</li>
 *   <li><b>Weekly progress</b>: distinct completion days in each Monday-Sunday week against the
 *       target; a week is met when {@code done >= target}. Targets are not prorated for partial weeks.</li>
 *   <li><b>Consistency</b>: completed days divided by active days (inside start/end, not paused).</li>
 * </ul>
 */
public final class StreakCalculator {

    private StreakCalculator() {
    }

    public record WeekProgress(LocalDate weekStart, int done, int target, boolean met) {
    }

    public record Consistency(int completedDays, int activeDays, BigDecimal percent) {
    }

    public static int streakEndingOn(Set<LocalDate> completions, HabitDefinition habit, LocalDate endDay) {
        if (!completions.contains(endDay) && !habit.isPaused(endDay)) {
            return 0;
        }
        int count = 0;
        LocalDate day = endDay;
        while (!day.isBefore(habit.startDate())) {
            if (habit.isPaused(day)) {
                day = day.minusDays(1);
            } else if (completions.contains(day)) {
                count++;
                day = day.minusDays(1);
            } else {
                break;
            }
        }
        return count;
    }

    public static int currentStreak(Set<LocalDate> completions, HabitDefinition habit, LocalDate today) {
        boolean todayOpen = !completions.contains(today) && !habit.isPaused(today);
        LocalDate anchor = todayOpen ? today.minusDays(1) : today;
        return streakEndingOn(completions, habit, anchor);
    }

    public static int longestStreak(Set<LocalDate> completions, HabitDefinition habit) {
        if (completions.isEmpty()) {
            return 0;
        }
        LocalDate first = Collections.min(completions);
        LocalDate last = Collections.max(completions);
        int run = 0;
        int best = 0;
        for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
            if (habit.isPaused(day)) {
                continue;
            }
            if (completions.contains(day)) {
                run++;
                best = Math.max(best, run);
            } else {
                run = 0;
            }
        }
        return best;
    }

    /** One entry per ISO week overlapping {@code [from, to]}, zero-filled, oldest first. */
    public static List<WeekProgress> weeklyProgress(Set<LocalDate> completions, HabitDefinition habit,
                                                    LocalDate from, LocalDate to) {
        DateRules.daysInclusive(from, to); // validates order
        List<WeekProgress> weeks = new ArrayList<>();
        for (LocalDate week = DateRules.weekStart(from); !week.isAfter(to); week = week.plusWeeks(1)) {
            weeks.add(progressForWeek(completions, habit, week));
        }
        return weeks;
    }

    /**
     * Consecutive met weeks counting back from this week, or from last week while this week is still
     * in progress and not yet met. Never counts weeks before the habit started.
     */
    public static int weeklyStreak(Set<LocalDate> completions, HabitDefinition habit, LocalDate today) {
        LocalDate week = DateRules.weekStart(today);
        if (!progressForWeek(completions, habit, week).met()) {
            week = week.minusWeeks(1);
        }
        LocalDate firstWeek = DateRules.weekStart(habit.startDate());
        int streak = 0;
        while (!week.isBefore(firstWeek) && progressForWeek(completions, habit, week).met()) {
            streak++;
            week = week.minusWeeks(1);
        }
        return streak;
    }

    /** Share of active days in {@code [from, to]} that were completed. Percent is 0.0 when no day was active. */
    public static Consistency consistency(Set<LocalDate> completions, HabitDefinition habit,
                                          LocalDate from, LocalDate to) {
        DateRules.daysInclusive(from, to);
        int active = 0;
        int done = 0;
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            if (habit.isActiveOn(day)) {
                active++;
                if (completions.contains(day)) {
                    done++;
                }
            }
        }
        return new Consistency(done, active, Numbers.percent(done, active, 1));
    }

    private static WeekProgress progressForWeek(Set<LocalDate> completions, HabitDefinition habit, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        int done = (int) completions.stream().filter(d -> DateRules.inRange(d, weekStart, weekEnd)).count();
        return new WeekProgress(weekStart, done, habit.targetCount(), done >= habit.targetCount());
    }
}
