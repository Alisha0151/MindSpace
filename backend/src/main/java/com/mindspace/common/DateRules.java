package com.mindspace.common;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * Date conventions used everywhere in MindSpace.
 *
 * <ul>
 *   <li>A "local date" is the calendar day in the user's IANA timezone.</li>
 *   <li>Weeks are ISO weeks: Monday through Sunday. A week is identified by its Monday.</li>
 *   <li>Date ranges are inclusive on both ends.</li>
 * </ul>
 */
public final class DateRules {

    private DateRules() {
    }

    /** The user's calendar day at the given instant. */
    public static LocalDate localDate(Instant instant, ZoneId zone) {
        return instant.atZone(zone).toLocalDate();
    }

    /** The Monday of the ISO week containing {@code date}. */
    public static LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** Number of days in {@code [from, to]}, both inclusive. */
    public static long daysInclusive(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("'to' (" + to + ") is before 'from' (" + from + ")");
        }
        return ChronoUnit.DAYS.between(from, to) + 1;
    }

    public static boolean inRange(LocalDate date, LocalDate from, LocalDate to) {
        return !date.isBefore(from) && !date.isAfter(to);
    }
}
