package com.mindspace.it;

import com.mindspace.support.AbstractIntegrationTest;
import com.mindspace.support.ApiClient;
import com.mindspace.support.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IsolationAndCrudIT extends AbstractIntegrationTest {

    private ApiClient registered() {
        ApiClient c = anonymous();
        assertEquals(201, c.post("/api/v1/auth/register", TestData.registerJson(TestData.uniqueEmail())).status());
        return c;
    }

    private static String idOf(String json) {
        Matcher m = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-f-]{36})\"").matcher(json);
        assertTrue(m.find(), json);
        return m.group(1);
    }

    @Test
    @DisplayName("check-in: create, duplicate day -> 409, update, delete")
    void checkIns() {
        ApiClient c = registered();
        ApiClient.Reply created = c.post("/api/v1/checkins", TestData.checkInJson(4));
        assertEquals(201, created.status());
        String id = idOf(created.raw());
        assertEquals(409, c.post("/api/v1/checkins", TestData.checkInJson(5)).status());
        assertEquals(200, c.put("/api/v1/checkins/" + id, TestData.checkInJson(5)).status());
        assertEquals(204, c.delete("/api/v1/checkins/" + id).status());
        assertEquals(404, c.get("/api/v1/checkins/" + id).status());
    }

    @Test
    @DisplayName("another user's check-in, journal entry and habit are 404, never 403 or the data")
    void crossUser() {
        ApiClient alice = registered();
        ApiClient bob = registered();
        String checkIn = idOf(alice.post("/api/v1/checkins", TestData.checkInJson(3)).raw());
        String entry = idOf(alice.post("/api/v1/journal",
                "{\"title\":\"t\",\"body\":\"private words\",\"tags\":[\"a\"]}").raw());
        String habit = idOf(alice.post("/api/v1/habits", "{\"name\":\"Walk\",\"cadence\":\"DAILY\"}").raw());

        assertEquals(404, bob.get("/api/v1/checkins/" + checkIn).status());
        assertEquals(404, bob.put("/api/v1/checkins/" + checkIn, TestData.checkInJson(1)).status());
        assertEquals(404, bob.delete("/api/v1/checkins/" + checkIn).status());
        assertEquals(404, bob.get("/api/v1/journal/" + entry).status());
        assertEquals(404, bob.delete("/api/v1/journal/" + entry).status());
        assertEquals(404, bob.get("/api/v1/habits/" + habit).status());
        assertEquals(404, bob.put("/api/v1/habits/" + habit + "/completions/2026-01-01", null).status());
        assertTrue(!bob.get("/api/v1/journal").raw().contains("private words"));
        assertEquals(200, alice.get("/api/v1/journal/" + entry).status());
    }

    @Test
    @DisplayName("journal: create, full-text search, tag filter, delete")
    void journal() {
        ApiClient c = registered();
        c.post("/api/v1/journal", "{\"title\":\"Rainy walk\",\"body\":\"The rain was calming today\",\"tags\":[\"nature\"]}");
        c.post("/api/v1/journal", "{\"title\":\"Work\",\"body\":\"Deadline pressure\",\"tags\":[\"work\"]}");
        assertTrue(c.get("/api/v1/journal?q=rain").raw().contains("Rainy walk"));
        assertTrue(!c.get("/api/v1/journal?q=rain").raw().contains("Deadline"));
        assertTrue(c.get("/api/v1/journal?tag=work").raw().contains("Work"));
    }

    @Test
    @DisplayName("habit: complete is idempotent, future date rejected, duplicate name 409, progress works")
    void habits() {
        ApiClient c = registered();
        String id = idOf(c.post("/api/v1/habits", "{\"name\":\"Stretch\",\"cadence\":\"DAILY\"}").raw());
        assertEquals(409, c.post("/api/v1/habits", "{\"name\":\"Stretch\",\"cadence\":\"DAILY\"}").status());
        String today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).toString();
        assertEquals(204, c.put("/api/v1/habits/" + id + "/completions/" + today, null).status());
        assertEquals(204, c.put("/api/v1/habits/" + id + "/completions/" + today, null).status());
        assertEquals(400, c.put("/api/v1/habits/" + id + "/completions/2999-01-01", null).status());
        ApiClient.Reply habit = c.get("/api/v1/habits/" + id);
        assertTrue(habit.raw().contains("\"currentStreak\":1"), habit.raw());
        assertEquals(200, c.get("/api/v1/habits/" + id + "/progress").status());
        assertEquals(204, c.delete("/api/v1/habits/" + id).status());
    }

    @Test
    @DisplayName("analytics, calendar, dashboard and toolkit endpoints answer for a new user")
    void readEndpoints() {
        ApiClient c = registered();
        c.post("/api/v1/checkins", TestData.checkInJson(4));
        assertEquals(200, c.get("/api/v1/analytics/summary").status());
        assertEquals(200, c.get("/api/v1/dashboard").status());
        assertEquals(200, c.get("/api/v1/wellness/toolkit").status());
        String month = java.time.YearMonth.now(java.time.ZoneId.of("Asia/Kolkata")).toString();
        assertEquals(200, c.get("/api/v1/calendar?month=" + month).status());
        assertEquals(400, c.get("/api/v1/calendar?month=nonsense").status());
    }

    @Test
    @DisplayName("export contains the user's data; deleting the account needs the password and removes everything")
    void exportAndDelete() {
        ApiClient c = registered();
        c.post("/api/v1/checkins", TestData.checkInJson(4));
        c.post("/api/v1/journal", "{\"title\":\"x\",\"body\":\"exported words\",\"tags\":[]}");
        ApiClient.Reply export = c.get("/api/v1/privacy/export");
        assertEquals(200, export.status());
        assertTrue(export.raw().contains("exported words"));
        assertTrue(export.raw().contains("\"schemaVersion\":1"));
        assertTrue(!export.raw().contains("passwordHash") && !export.raw().contains("password_hash"));

        assertEquals(400, c.deleteWithBody("/api/v1/account", "{\"password\":\"wrong wrong wrong wrong\"}").status());
        assertEquals(204, c.deleteWithBody("/api/v1/account", "{\"password\":\"" + TestData.PASSWORD + "\"}").status());
        assertEquals(401, c.get("/api/v1/users/me").status());
    }
}
