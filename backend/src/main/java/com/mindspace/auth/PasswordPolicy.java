package com.mindspace.auth;

import com.mindspace.common.FieldViolation;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Password policy. Length-based with a small blocklist rather than character-class rules
 * (following current NIST SP 800-63B guidance). The 72-byte ceiling exists because BCrypt
 * ignores everything after byte 72; rejecting longer input avoids silently truncating passwords.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_BYTES = 72;

    private static final Set<String> COMMON = Set.of(
            "password1234", "123456789012", "qwertyuiop12", "iloveyou1234", "administrator",
            "letmein12345", "welcome12345", "passwordpassword", "1234567890123", "abcdefghijkl");

    private PasswordPolicy() {
    }

    public static List<FieldViolation> validate(String password, String email, String displayName) {
        List<FieldViolation> problems = new ArrayList<>();
        if (password == null || password.isEmpty()) {
            problems.add(new FieldViolation("password", "is required"));
            return problems;
        }
        if (password.codePointCount(0, password.length()) < MIN_LENGTH) {
            problems.add(new FieldViolation("password", "must be at least " + MIN_LENGTH + " characters"));
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            problems.add(new FieldViolation("password", "must be at most " + MAX_BYTES + " bytes (about 72 characters)"));
        }
        String lower = password.toLowerCase(Locale.ROOT);
        if (COMMON.contains(lower) || password.chars().distinct().count() < 4) {
            problems.add(new FieldViolation("password", "is too easy to guess"));
        }
        if (email != null) {
            String local = email.toLowerCase(Locale.ROOT);
            int at = local.indexOf('@');
            if (at > 0) {
                local = local.substring(0, at);
            }
            if (local.length() >= 4 && lower.contains(local)) {
                problems.add(new FieldViolation("password", "must not contain your e-mail name"));
            }
        }
        if (displayName != null && displayName.strip().length() >= 4
                && lower.contains(displayName.strip().toLowerCase(Locale.ROOT))) {
            problems.add(new FieldViolation("password", "must not contain your name"));
        }
        return problems;
    }
}
