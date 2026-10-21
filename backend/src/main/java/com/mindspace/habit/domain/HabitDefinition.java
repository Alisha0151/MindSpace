package com.mindspace.habit.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * The definition of a habit as the domain logic sees it. Invariants mirror {@code V4__habits.sql}:
 * DAILY needs target 1, WEEKLY needs 1..7, {@code end >= start}.
 *
 * <p>Completions are deliberately not part of the definition so history stays meaningful.
 */
public record HabitDefinition(Cadence cadence, int targetCount, LocalDate startDate,
                              LocalDate endDate, List<PauseWindow> pauses) {

    public HabitDefinition {
        if (cadence == null || startDate == null) {
            throw new IllegalArgumentException("cadence and startDate are required");
        }
        if (cadence == Cadence.DAILY && targetCount != 1) {
            throw new IllegalArgumentException("a DAILY habit must have targetCount 1");
        }
        if (cadence == Cadence.WEEKLY && (targetCount < 1 || targetCount > 7)) {
            throw new IllegalArgumentException("a WEEKLY habit must have targetCount between 1 and 7");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
        pauses = pauses == null ? List.of() : List.copyOf(pauses);
    }

    public boolean isPaused(LocalDate day) {
        return pauses.stream().anyMatch(p -> p.contains(day));
    }

    /** True when the habit is active on {@code day}: inside start/end and not paused. */
    public boolean isActiveOn(LocalDate day) {
        return !day.isBefore(startDate) && (endDate == null || !day.isAfter(endDate)) && !isPaused(day);
    }
}
