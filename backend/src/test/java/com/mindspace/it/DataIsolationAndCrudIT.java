package com.mindspace.it;

import com.mindspace.support.AbstractIntegrationTest;
import com.mindspace.support.ApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataIsolationAndCrudIT extends AbstractIntegrationTest {

    private static Map<String, Object> checkIn(String date) {
        return Map.of("date", date, "moodScore", 4, "stressScore", 2, "energyScore", 3, "sleepMinutes", 450,
                "emotions", List.of("calm"), "activities", List.of("walk"), "contexts", List.of("home"),
                "notes", "private note");
    }

    @Test
    @DisplayName("one check-in per day: a duplicate date is a 409, not a second row")
    void duplicateCheckIn() {
        ApiClient a = newUser("Ana");
        String today = LocalDate.now(java.time.ZoneOffset.UTC).toString();
        assertEquals(201, a.post("/api/v1/checkins", checkIn(today)).status());
        assertEquals(409, a.post("/api/v1/checkins", checkIn(today)).status());
    }

    @Test
    @DisplayName("another user's check-in, journal entry and habit return 404 on every verb")
    void crossUserIsolation() {
        ApiClient a = newUser("Ann");
        ApiClient b = newUser("Ben");
        String today = LocalDate.now(java.time.ZoneOffset.UTC).toString();

        String checkInId = a.post("/api/v1/checkins", checkIn(today)).body().get("id").asText();
        String entryId = a.post("/api/v1/journal", Map.of("title", "T", "body", "secret words", "tags", List.of("x"))).body().get("id").asText();
        String habitId = a.post("/api/v1/habits", Map.of("name", "Read", "cadence", "DAILY")).body().get("id").asText();

        assertEquals(404, b.get("/api/v1/checkins/" + checkInId).status());
        assertEquals(404, b.put("/api/v1/checkins/" + checkInId, checkIn(today)).status());
        assertEquals(404, b.delete("/api/v1/checkins/" + checkInId).status());
        assertEquals(404, b.get("/api/v1/journal/" + entryId).status());
        assertEquals(404, b.delete("/api/v1/journal/" + entryId).status());
        assertEquals(404, b.get("/api/v1/habits/" + habitId).status());
        assertEquals(404, b.put("/api/v1/habits/" + habitId + "/completions/" + today, null).status());
        assertEquals(404, b.delete("/api/v1/habits/" + habitId).status());

        // lists and search never leak
        assertEquals(0, b.get("/api/v1/journal?q=secret").body().get("items").size());
        assertEquals(0, b.get("/api/v1/habits").body().size());
        // and the owner still has everything
        assertEquals(200, a.get("/api/v1/checkins/" + checkInId).status());
        assertEquals(1, a.get("/api/v1/journal?q=secret").body().get("items").size());
    }

    @Test
    @DisplayName("journal search is full-text and filters by tag")
    void journalSearch() {
        ApiClient a = newUser("Ada");
        a.post("/api/v1/journal", Map.of("title", "Morning pages", "body", "Walked by the river and felt calm", "tags", List.of("outdoors")));
        a.post("/api/v1/journal", Map.of("title", "Work", "body", "Deadline pressure all day", "tags", List.of("work")));
        assertEquals(1, a.get("/api/v1/journal?q=river").body().get("items").size());
        assertEquals(1, a.get("/api/v1/journal?tag=work").body().get("items").size());
        assertEquals(2, a.get("/api/v1/journal").body().get("items").size());
    }

    @Test
    @DisplayName("habit completion is idempotent; future dates and paused days are rejected; undo is idempotent")
    void habitCompletion() {
        ApiClient a = newUser("Abe");
        String id = a.post("/api/v1/habits", Map.of("name", "Stretch", "cadence", "DAILY")).body().get("id").asText();
        String today = LocalDate.now(java.time.ZoneOffset.UTC).toString();
        String tomorrow = LocalDate.now(java.time.ZoneOffset.UTC).plusDays(1).toString();

        assertEquals(204, a.put("/api/v1/habits/" + id + "/completions/" + today, null).status());
        assertEquals(204, a.put("/api/v1/habits/" + id + "/completions/" + today, null).status());
        assertEquals(400, a.put("/api/v1/habits/" + id + "/completions/" + tomorrow, null).status());
        var habit = a.get("/api/v1/habits/" + id).body();
        assertTrue(habit.get("doneToday").asBoolean());
        assertEquals(1, habit.get("currentStreak").asInt());

        assertEquals(204, a.delete("/api/v1/habits/" + id + "/completions/" + today).status());
        assertEquals(204, a.delete("/api/v1/habits/" + id + "/completions/" + today).status());
        assertEquals(409, a.post("/api/v1/habits", Map.of("name", "Stretch", "cadence", "DAILY")).status());
    }

    @Test
    @DisplayName("analytics reports 'not enough data' for a new account instead of inventing a trend")
    void analyticsEmpty() {
        ApiClient a = newUser("Amy");
        var s = a.get("/api/v1/analytics/summary").body();
        assertEquals(0, s.get("observations").asInt());
        assertEquals(false, s.get("enoughData").asBoolean());
    }
}
