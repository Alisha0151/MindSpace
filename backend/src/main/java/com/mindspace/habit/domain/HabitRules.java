package com.mindspace.habit.domain;

import com.mindspace.common.FieldViolation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Rules the database cannot express as a single-table CHECK (see the header of
 * {@code V4__habits.sql}): whether a completion may be recorded for a given day.
 */
public final class HabitRules {

    private HabitRules() {
    }

    /**
     * @param today the user's current local date (derive it with {@code DateRules.localDate})
     */
    public static List<FieldViolation> completionViolations(HabitDefinition habit, LocalDate day, LocalDate today) {
        List<FieldViolation> problems = new ArrayList<>();
        if (day == null) {
            problems.add(new FieldViolation("date", "is required"));
            return problems;
        }
        if (day.isAfter(today)) {
            problems.add(new FieldViolation("date", "cannot be in the future"));
        }
        if (day.isBefore(habit.startDate())) {
            problems.add(new FieldViolation("date", "is before the habit's start date"));
        }
        if (habit.endDate() != null && day.isAfter(habit.endDate())) {
            problems.add(new FieldViolation("date", "is after the habit's end date"));
        }
        if (habit.isPaused(day)) {
            problems.add(new FieldViolation("date", "falls in a period when the habit was paused"));
        }
        return problems;
    }
}
