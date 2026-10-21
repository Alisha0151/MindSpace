package com.mindspace.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DateRangeTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 15);

    @Test
    @DisplayName("no bounds -> the last N days ending today, inclusive")
    void defaults() {
        DateRange r = DateRange.resolve(null, null, TODAY, 30, 366);
        assertEquals(TODAY, r.to());
        assertEquals(TODAY.minusDays(29), r.from());
    }

    @Test
    @DisplayName("only 'from' -> N days from it, but never past today")
    void fromOnly() {
        assertEquals(new DateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 15)),
                DateRange.resolve(LocalDate.of(2026, 3, 1), null, TODAY, 30, 366));
        assertEquals(new DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 30)),
                DateRange.resolve(LocalDate.of(2026, 1, 1), null, TODAY, 30, 366));
    }

    @Test
    @DisplayName("only 'to' -> N days ending there")
    void toOnly() {
        assertEquals(new DateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 2)),
                DateRange.resolve(null, LocalDate.of(2026, 3, 2), TODAY, 30, 366));
    }

    @Test
    @DisplayName("to before from is rejected")
    void reversed() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> DateRange.resolve(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 1), TODAY, 30, 366));
        assertEquals("to", e.violations().get(0).field());
    }

    @Test
    @DisplayName("exactly the maximum is allowed, one more day is rejected")
    void cap() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        DateRange.resolve(from, from.plusDays(365), TODAY, 30, 366);
        assertThrows(ValidationException.class, () -> DateRange.resolve(from, from.plusDays(366), TODAY, 30, 366));
    }

    @Test
    @DisplayName("a single-day range is valid")
    void singleDay() {
        assertEquals(new DateRange(TODAY, TODAY), DateRange.resolve(TODAY, TODAY, TODAY, 30, 366));
    }
}
