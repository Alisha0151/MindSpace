package com.mindspace.common;

import java.time.LocalDate;

/** An inclusive local-date range, resolved from optional query parameters with a hard size cap. */
public record DateRange(LocalDate from, LocalDate to) {

    /**
     * @param today        the user's current local date
     * @param defaultDays  length used when a bound is missing (e.g. 30 = last 30 days including today)
     * @param maxDays      largest allowed range (protects the database from unbounded queries)
     */
    public static DateRange resolve(LocalDate from, LocalDate to, LocalDate today, int defaultDays, int maxDays) {
        LocalDate resolvedTo = to != null ? to : (from != null ? minDate(today, from.plusDays(defaultDays - 1L)) : today);
        LocalDate resolvedFrom = from != null ? from : resolvedTo.minusDays(defaultDays - 1L);
        if (resolvedTo.isBefore(resolvedFrom)) {
            throw new ValidationException("to", "must not be before 'from'");
        }
        if (DateRules.daysInclusive(resolvedFrom, resolvedTo) > maxDays) {
            throw new ValidationException("from", "range must not exceed " + maxDays + " days");
        }
        return new DateRange(resolvedFrom, resolvedTo);
    }

    private static LocalDate minDate(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
