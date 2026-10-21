package com.mindspace.analytics.domain;

import java.time.LocalDate;
import java.util.Set;

/**
 * The slice of a check-in that analytics needs. Contains no notes or other free text.
 * Callers must pass only the authenticated user's own check-ins.
 */
public record CheckInPoint(LocalDate date, int mood, int stress, int energy,
                           int sleepMinutes, Set<String> emotions) {

    public CheckInPoint {
        emotions = emotions == null ? Set.of() : Set.copyOf(emotions);
    }
}
