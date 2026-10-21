package com.mindspace.analytics.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * The same synthetic data set as {@code db/tests/60_analytics.sql}, so the Java calculators can be
 * checked against numbers that were already verified in PostgreSQL.
 */
final class Fixtures {

    private Fixtures() {
    }

    static LocalDate oct(int day) {
        return LocalDate.of(2026, 10, day);
    }

    private static CheckInPoint p(int day, int mood, int stress, int energy, int sleep, String... emotions) {
        return new CheckInPoint(oct(day), mood, stress, energy, sleep, Set.of(emotions));
    }

    static List<CheckInPoint> userA() {
        return List.of(
                p(5, 4, 2, 4, 450, "calm", "hopeful"),
                p(6, 3, 3, 3, 390, "calm"),
                p(7, 5, 1, 5, 480, "joyful", "calm"),
                p(8, 2, 4, 2, 300, "anxious"),
                p(9, 3, 3, 3, 420, "calm", "tired"),
                p(12, 4, 2, 4, 460, "hopeful", "calm"),
                p(13, 4, 2, 4, 440, "calm"),
                p(14, 1, 5, 1, 240, "anxious", "sad"),
                p(15, 2, 4, 2, 420, "tired"),
                p(18, 5, 1, 5, 500, "joyful", "hopeful"));
    }

    /** "Evening walk" completions from the SQL fixture. */
    static Set<LocalDate> eveningWalk() {
        return Set.of(oct(5), oct(7), oct(9), oct(10), oct(11), oct(12), oct(13), oct(18));
    }

    /** "Long run" completions from the SQL fixture. */
    static Set<LocalDate> longRun() {
        return Set.of(oct(6), oct(8), oct(14));
    }
}
