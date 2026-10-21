package com.mindspace.habit.domain;

import java.time.LocalDate;

/**
 * A period during which a habit was paused: {@code [pausedOn, resumedOn)} (resume day is active again).
 * {@code resumedOn == null} means the habit is still paused.
 */
public record PauseWindow(LocalDate pausedOn, LocalDate resumedOn) {

    public PauseWindow {
        if (pausedOn == null) {
            throw new IllegalArgumentException("pausedOn is required");
        }
        if (resumedOn != null && !resumedOn.isAfter(pausedOn)) {
            throw new IllegalArgumentException("resumedOn must be after pausedOn");
        }
    }

    public boolean contains(LocalDate day) {
        return !day.isBefore(pausedOn) && (resumedOn == null || day.isBefore(resumedOn));
    }
}
