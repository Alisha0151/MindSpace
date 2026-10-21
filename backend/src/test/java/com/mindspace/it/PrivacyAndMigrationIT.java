package com.mindspace.it;

import com.mindspace.support.AbstractIntegrationTest;
import com.mindspace.support.ApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivacyAndMigrationIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("all six Flyway migrations applied and the schema passed Hibernate validation (context started)")
    void migrations() {
        Integer applied = jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class);
        assertEquals(6, applied);
    }

    @Test
    @DisplayName("export contains the user's data and never a password hash or token hash")
    void export() {
        ApiClient a = newUser("Eli");
        a.post("/api/v1/journal", Map.of("title", "Hello", "body", "export me", "tags", List.of()));
        ApiClient.Reply r = a.get("/api/v1/privacy/export");
        assertEquals(200, r.status());
        assertEquals(1, r.body().get("schemaVersion").asInt());
        assertEquals(1, r.body().get("journalEntries").size());
        assertFalse(r.raw().contains("password_hash"));
        assertFalse(r.raw().toLowerCase().contains("tokenhash"));
    }

    @Test
    @DisplayName("account deletion needs the password and then removes every owned row")
    void deleteAccount() {
        ApiClient a = newUser("Ola");
        String id = a.get("/api/v1/users/me").body().get("id").asText();
        a.post("/api/v1/journal", Map.of("title", "x", "body", "y", "tags", List.of("t")));
        a.post("/api/v1/habits", Map.of("name", "H", "cadence", "DAILY"));

        assertEquals(400, a.deleteWithBody("/api/v1/account", Map.of("password", "wrong-password-wrong")).status());
        assertEquals(204, a.deleteWithBody("/api/v1/account", Map.of("password", PASSWORD)).status());

        for (String table : List.of("users", "journal_entries", "habits", "refresh_sessions", "consent_records")) {
            String col = table.equals("users") ? "id" : "user_id";
            Integer n = jdbc.queryForObject("select count(*) from " + table + " where " + col + " = ?::uuid", Integer.class, id);
            assertEquals(0, n, table + " should be empty after deletion");
        }
        assertTrue(a.get("/api/v1/users/me").status() == 401);
    }
}
