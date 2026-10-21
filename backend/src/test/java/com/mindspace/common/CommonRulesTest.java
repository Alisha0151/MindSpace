package com.mindspace.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonRulesTest {

    @Test
    @DisplayName("the same instant is different calendar days in different timezones")
    void localDateDependsOnZone() {
        Instant instant = Instant.parse("2026-10-17T19:00:00Z");
        assertEquals(LocalDate.of(2026, 10, 18), DateRules.localDate(instant, ZoneId.of("Asia/Kolkata")));
        assertEquals(LocalDate.of(2026, 10, 17), DateRules.localDate(instant, ZoneId.of("UTC")));
        assertEquals(LocalDate.of(2026, 10, 17), DateRules.localDate(instant, ZoneId.of("America/Los_Angeles")));
        assertEquals(LocalDate.of(2026, 10, 18), DateRules.localDate(instant, ZoneId.of("Pacific/Kiritimati")));
    }

    @Test
    @DisplayName("local date is correct across a DST change (US fall-back, 2026-11-01)")
    void localDateAcrossDst() {
        ZoneId la = ZoneId.of("America/Los_Angeles");
        // 2026-11-01T08:30Z is 01:30 PDT (first pass); 09:30Z is 01:30 PST (second pass). Same calendar day.
        assertEquals(LocalDate.of(2026, 11, 1), DateRules.localDate(Instant.parse("2026-11-01T08:30:00Z"), la));
        assertEquals(LocalDate.of(2026, 11, 1), DateRules.localDate(Instant.parse("2026-11-01T09:30:00Z"), la));
        assertEquals(LocalDate.of(2026, 10, 31), DateRules.localDate(Instant.parse("2026-11-01T06:59:00Z"), la));
    }

    @Test
    @DisplayName("weeks start on Monday")
    void weekStart() {
        assertEquals(LocalDate.of(2026, 10, 12), DateRules.weekStart(LocalDate.of(2026, 10, 12))); // Monday
        assertEquals(LocalDate.of(2026, 10, 12), DateRules.weekStart(LocalDate.of(2026, 10, 18))); // Sunday
        assertEquals(LocalDate.of(2026, 10, 5), DateRules.weekStart(LocalDate.of(2026, 10, 11)));  // Sunday
        assertEquals(LocalDate.of(2025, 12, 29), DateRules.weekStart(LocalDate.of(2026, 1, 1)));   // year boundary
    }

    @Test
    @DisplayName("inclusive day counts")
    void daysInclusive() {
        assertEquals(1, DateRules.daysInclusive(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 5)));
        assertEquals(14, DateRules.daysInclusive(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 18)));
        assertThrows(IllegalArgumentException.class,
                () -> DateRules.daysInclusive(LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 5)));
    }

    @Test
    @DisplayName("average and percent: HALF_UP, null/zero on empty, no division by zero")
    void numbers() {
        assertNull(Numbers.average(0, 0, 2));
        assertEquals(new BigDecimal("2.13"), Numbers.average(17, 8, 2));
        assertEquals(new BigDecimal("4.17"), Numbers.average(25, 6, 2));
        assertEquals(new BigDecimal("0.0"), Numbers.percent(0, 0, 1));
        assertEquals(new BigDecimal("71.4"), Numbers.percent(10, 14, 1));
    }

    @Test
    @DisplayName("blank detection treats tab and newline as whitespace (matches the DB constraint)")
    void blank() {
        assertTrue(Text.isBlank(null));
        assertTrue(Text.isBlank(" \t\r\n "));
        assertFalse(Text.isBlank(" a "));
        assertEquals("a b", Text.trim(" \t a b\n"));
    }

    @Test
    @DisplayName("e-mail normalisation")
    void email() {
        assertEquals("a.b@example.test", Text.normalizeEmail("  A.B@Example.TEST \n"));
        assertThrows(IllegalArgumentException.class, () -> Text.normalizeEmail("no-at-sign"));
        assertThrows(IllegalArgumentException.class, () -> Text.normalizeEmail("@x.test"));
        assertThrows(IllegalArgumentException.class, () -> Text.normalizeEmail(null));
    }

    @Test
    @DisplayName("tag normalisation")
    void tag() {
        assertEquals("calm", Text.normalizeTag("  Calm "));
        assertThrows(IllegalArgumentException.class, () -> Text.normalizeTag("   "));
        assertThrows(IllegalArgumentException.class, () -> Text.normalizeTag("x".repeat(51)));
        assertEquals("x".repeat(50), Text.normalizeTag("x".repeat(50)));
    }
}
