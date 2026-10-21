package com.mindspace.auth;

import com.mindspace.common.FieldViolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordAndTokenTest {

    @Test
    @DisplayName("a long unique passphrase is accepted")
    void accepts() {
        assertEquals(List.of(), PasswordPolicy.validate("correct horse battery staple", "sam@example.test", "Sam Synthetic"));
    }

    @Test
    @DisplayName("passwords shorter than 12 characters are rejected")
    void tooShort() {
        List<FieldViolation> v = PasswordPolicy.validate("short-pass1", "sam@example.test", "Sam");
        assertEquals(List.of(new FieldViolation("password", "must be at least 12 characters")), v);
    }

    @Test
    @DisplayName("passwords over 72 bytes are rejected (BCrypt would silently truncate them)")
    void tooLong() {
        List<FieldViolation> v = PasswordPolicy.validate("a1b2".repeat(19), "x@example.test", "Sam");
        assertTrue(v.stream().anyMatch(f -> f.message().contains("72 bytes")));
        assertEquals(List.of(), PasswordPolicy.validate("ab1c".repeat(18), "x@example.test", "Sam")); // exactly 72 bytes
    }

    @Test
    @DisplayName("multi-byte characters count as bytes for the 72-byte limit")
    void multiByte() {
        String emoji = "é".repeat(37) + "xyz"; // 37 * 2 + 3 = 77 bytes, only 40 characters
        assertTrue(PasswordPolicy.validate(emoji, "x@example.test", "Sam").stream()
                .anyMatch(f -> f.message().contains("72 bytes")));
    }

    @Test
    @DisplayName("common and low-variety passwords are rejected")
    void guessable() {
        assertFalse(PasswordPolicy.validate("passwordpassword", "x@example.test", "Sam").isEmpty());
        assertFalse(PasswordPolicy.validate("aaaaaaaaaaaaaaaa", "x@example.test", "Sam").isEmpty());
    }

    @Test
    @DisplayName("passwords containing the e-mail name or display name are rejected")
    void personalInfo() {
        assertFalse(PasswordPolicy.validate("my-samantha-secret-1", "samantha@example.test", "Other").isEmpty());
        assertFalse(PasswordPolicy.validate("totally-Jordan-secret", "x@example.test", "Jordan").isEmpty());
    }

    @Test
    @DisplayName("missing password is reported once")
    void missing() {
        assertEquals(List.of(new FieldViolation("password", "is required")), PasswordPolicy.validate(null, "x@example.test", "Sam"));
    }

    @Test
    @DisplayName("sha256Hex of a known value matches the published test vector")
    void knownVector() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", TokenHasher.sha256Hex("abc"));
    }

    @Test
    @DisplayName("sha256Hex output matches the 64-char lower-case hex CHECK on refresh_sessions.token_hash")
    void matchesDbCheck() {
        assertTrue(TokenHasher.sha256Hex(TokenHasher.newToken()).matches("^[0-9a-f]{64}$"));
    }

    @Test
    @DisplayName("tokens are 43 URL-safe characters and unique")
    void tokens() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String t = TokenHasher.newToken();
            assertEquals(43, t.length());
            assertTrue(t.matches("^[A-Za-z0-9_-]+$"));
            assertTrue(seen.add(t));
        }
    }
}
