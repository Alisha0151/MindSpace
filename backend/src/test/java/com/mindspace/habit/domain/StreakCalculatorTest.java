package com.mindspace.habit.domain;

import com.mindspace.common.FieldViolation;
import com.mindspace.habit.domain.StreakCalculator.Consistency;
import com.mindspace.habit.domain.StreakCalculator.WeekProgress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreakCalculatorTest {

    private static LocalDate oct(int day) {
        return LocalDate.of(2026, 10, day);
    }

    /** Completions from the SQL fixture: 5 7 9 10 11 12 13 18. */
    private static final Set<LocalDate> WALK = Set.of(oct(5), oct(7), oct(9), oct(10), oct(11), oct(12), oct(13), oct(18));
    private static final HabitDefinition DAILY = new HabitDefinition(Cadence.DAILY, 1, oct(5), null, List.of());
    private static final HabitDefinition WEEKLY2 = new HabitDefinition(Cadence.WEEKLY, 2, oct(5), null, List.of());

    @Test
    @DisplayName("longest daily streak is 5 (Oct 9-13)")
    void longest() {
        assertEquals(5, StreakCalculator.longestStreak(WALK, DAILY));
        assertEquals(0, StreakCalculator.longestStreak(Set.of(), DAILY));
    }

    @Test
    @DisplayName("streak ending on a day: 5 on Oct 13, 1 on Oct 18, 0 on a missed day")
    void streakEnding() {
        assertEquals(5, StreakCalculator.streakEndingOn(WALK, DAILY, oct(13)));
        assertEquals(1, StreakCalculator.streakEndingOn(WALK, DAILY, oct(18)));
        assertEquals(0, StreakCalculator.streakEndingOn(WALK, DAILY, oct(17)));
    }

    @Test
    @DisplayName("current streak: today not done yet does not reset the streak")
    void currentStreakGrace() {
        assertEquals(5, StreakCalculator.currentStreak(WALK, DAILY, oct(14))); // 14th still open, 9-13 counts
        assertEquals(0, StreakCalculator.currentStreak(WALK, DAILY, oct(15))); // 14th was missed too
        assertEquals(1, StreakCalculator.currentStreak(WALK, DAILY, oct(18)));
    }

    @Test
    @DisplayName("paused days neither count nor break a streak")
    void pausesAreSkipped() {
        // done Oct 1,2, paused Oct 3-4 (resumed Oct 5), done Oct 5,6
        Set<LocalDate> done = Set.of(oct(1), oct(2), oct(5), oct(6));
        HabitDefinition h = new HabitDefinition(Cadence.DAILY, 1, oct(1), null,
                List.of(new PauseWindow(oct(3), oct(5))));
        assertEquals(4, StreakCalculator.streakEndingOn(done, h, oct(6)));
        assertEquals(4, StreakCalculator.longestStreak(done, h));
        // Without the pause the gap breaks it
        HabitDefinition noPause = new HabitDefinition(Cadence.DAILY, 1, oct(1), null, List.of());
        assertEquals(2, StreakCalculator.streakEndingOn(done, noPause, oct(6)));
        assertEquals(2, StreakCalculator.longestStreak(done, noPause));
    }

    @Test
    @DisplayName("a streak never counts days before the habit started")
    void startBoundary() {
        HabitDefinition h = new HabitDefinition(Cadence.DAILY, 1, oct(6), null, List.of());
        assertEquals(2, StreakCalculator.streakEndingOn(Set.of(oct(5), oct(6), oct(7)), h, oct(7)));
    }

    @Test
    @DisplayName("weekly progress: week of Oct 5 is 2/2 met, week of Oct 12 is 1/2 not met")
    void weeklyProgress() {
        List<WeekProgress> weeks = StreakCalculator.weeklyProgress(Set.of(oct(6), oct(8), oct(14)), WEEKLY2, oct(5), oct(18));
        assertEquals(List.of(new WeekProgress(oct(5), 2, 2, true), new WeekProgress(oct(12), 1, 2, false)), weeks);
    }

    @Test
    @DisplayName("weekly progress is zero-filled for weeks with no completions and spans partial weeks")
    void weeklyProgressZeroFilled() {
        List<WeekProgress> weeks = StreakCalculator.weeklyProgress(Set.of(), WEEKLY2, oct(7), oct(21));
        assertEquals(3, weeks.size()); // weeks of Oct 5, 12, 19
        assertEquals(oct(5), weeks.get(0).weekStart());
        assertTrue(weeks.stream().allMatch(w -> w.done() == 0 && !w.met()));
    }

    @Test
    @DisplayName("weekly streak: unfinished current week does not reset it")
    void weeklyStreak() {
        Set<LocalDate> done = Set.of(oct(6), oct(8), oct(14));
        assertEquals(1, StreakCalculator.weeklyStreak(done, WEEKLY2, oct(18))); // this week 1/2, last week met
        assertEquals(2, StreakCalculator.weeklyStreak(Set.of(oct(6), oct(8), oct(13), oct(14)), WEEKLY2, oct(18)));
        assertEquals(0, StreakCalculator.weeklyStreak(Set.of(), WEEKLY2, oct(18)));
    }

    @Test
    @DisplayName("consistency excludes paused days and days outside start/end from the denominator")
    void consistency() {
        HabitDefinition h = new HabitDefinition(Cadence.DAILY, 1, oct(5), null,
                List.of(new PauseWindow(oct(10), oct(12))));
        // Oct 5..18 = 14 days, minus paused Oct 10,11 = 12 active; completed on active days: 5,7,9,12,13,18 = 6
        assertEquals(new Consistency(6, 12, new BigDecimal("50.0")),
                StreakCalculator.consistency(WALK, h, oct(5), oct(18)));
    }

    @Test
    @DisplayName("consistency with no active days is 0.0%, not an error")
    void consistencyNoActiveDays() {
        HabitDefinition h = new HabitDefinition(Cadence.DAILY, 1, oct(20), null, List.of());
        assertEquals(new Consistency(0, 0, new BigDecimal("0.0")),
                StreakCalculator.consistency(Set.of(), h, oct(5), oct(18)));
    }

    @Test
    @DisplayName("completion rules: future, before start, after end, paused")
    void completionRules() {
        HabitDefinition h = new HabitDefinition(Cadence.DAILY, 1, oct(5), oct(20),
                List.of(new PauseWindow(oct(10), oct(12))));
        LocalDate today = oct(15);
        assertEquals(List.of(), HabitRules.completionViolations(h, oct(9), today));
        assertEquals(List.of(new FieldViolation("date", "cannot be in the future")),
                HabitRules.completionViolations(h, oct(16), today));
        assertEquals(List.of(new FieldViolation("date", "is before the habit's start date")),
                HabitRules.completionViolations(h, oct(4), today));
        assertEquals(List.of(new FieldViolation("date", "falls in a period when the habit was paused")),
                HabitRules.completionViolations(h, oct(11), today));
        assertEquals(List.of(), HabitRules.completionViolations(h, oct(12), today)); // resume day is active
        assertEquals(List.of(new FieldViolation("date", "is after the habit's end date")),
                HabitRules.completionViolations(h, oct(21), oct(25)));
    }

    @Test
    @DisplayName("definition invariants mirror the database CHECK constraints")
    void definitionInvariants() {
        assertThrows(IllegalArgumentException.class, () -> new HabitDefinition(Cadence.DAILY, 2, oct(1), null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new HabitDefinition(Cadence.WEEKLY, 0, oct(1), null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new HabitDefinition(Cadence.WEEKLY, 8, oct(1), null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new HabitDefinition(Cadence.DAILY, 1, oct(2), oct(1), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new PauseWindow(oct(5), oct(5)));
    }
}
