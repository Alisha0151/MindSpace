package com.mindspace.checkin.domain;

import com.mindspace.common.FieldViolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CheckInRulesTest {

    @Test
    @DisplayName("a valid check-in has no violations; sleep quality and notes are optional")
    void valid() {
        assertEquals(List.of(), CheckInRules.validate(4, 2, 3, 450, 4, "synthetic note"));
        assertEquals(List.of(), CheckInRules.validate(1, 1, 1, 0, null, null));
        assertEquals(List.of(), CheckInRules.validate(5, 5, 5, 1440, 5, "x".repeat(2000)));
    }

    @Test
    @DisplayName("scores outside 1..5 are rejected per field")
    void scoreBounds() {
        List<FieldViolation> v = CheckInRules.validate(0, 6, 3, 400, 7, null);
        assertEquals(List.of(
                new FieldViolation("moodScore", "must be between 1 and 5"),
                new FieldViolation("stressScore", "must be between 1 and 5"),
                new FieldViolation("sleepQuality", "must be between 1 and 5")), v);
    }

    @Test
    @DisplayName("required fields are reported when missing")
    void required() {
        List<FieldViolation> v = CheckInRules.validate(null, null, null, null, null, null);
        assertEquals(List.of(
                new FieldViolation("moodScore", "is required"),
                new FieldViolation("stressScore", "is required"),
                new FieldViolation("energyScore", "is required"),
                new FieldViolation("sleepMinutes", "is required")), v);
    }

    @Test
    @DisplayName("sleep minutes limited to 0..1440; notes limited to 2000 characters")
    void limits() {
        assertEquals(List.of(new FieldViolation("sleepMinutes", "must be between 0 and 1440")),
                CheckInRules.validate(3, 3, 3, -1, null, null));
        assertEquals(List.of(new FieldViolation("sleepMinutes", "must be between 0 and 1440")),
                CheckInRules.validate(3, 3, 3, 1441, null, null));
        assertEquals(List.of(new FieldViolation("notes", "must be at most 2000 characters")),
                CheckInRules.validate(3, 3, 3, 400, null, "x".repeat(2001)));
    }

    @Test
    @DisplayName("violation messages never echo submitted notes")
    void noEcho() {
        String secret = "my-private-note-" + "y".repeat(2000);
        List<FieldViolation> v = CheckInRules.validate(3, 3, 3, 400, null, secret);
        assertEquals(1, v.size());
        assertEquals(false, v.get(0).message().contains("my-private-note"));
    }
}
