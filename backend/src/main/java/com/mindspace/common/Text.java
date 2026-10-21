package com.mindspace.common;

import java.util.Locale;

/**
 * Text normalisation that matches the database CHECK constraints in the Flyway migrations
 * (whitespace means space, tab, CR or LF; e-mails and tags are stored lower-case and trimmed).
 */
public final class Text {

    private static final String DB_WHITESPACE = " \t\r\n";

    private Text() {
    }

    /** Trims the same characters PostgreSQL's {@code btrim(x, E' \t\r\n')} trims. */
    public static String trim(String value) {
        if (value == null) {
            return null;
        }
        int start = 0;
        int end = value.length();
        while (start < end && DB_WHITESPACE.indexOf(value.charAt(start)) >= 0) {
            start++;
        }
        while (end > start && DB_WHITESPACE.indexOf(value.charAt(end - 1)) >= 0) {
            end--;
        }
        return value.substring(start, end);
    }

    public static boolean isBlank(String value) {
        return value == null || trim(value).isEmpty();
    }

    /** Lower-cased, trimmed e-mail; throws if it could not satisfy the {@code users_email_normalized_chk} constraint. */
    public static String normalizeEmail(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("email is required");
        }
        String email = trim(raw).toLowerCase(Locale.ROOT);
        if (email.length() < 3 || email.length() > 320 || email.indexOf('@') < 1) {
            throw new IllegalArgumentException("email is not valid");
        }
        return email;
    }

    /** Lower-cased, trimmed tag of 1..50 characters (matches the tag CHECK constraints). */
    public static String normalizeTag(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("tag is required");
        }
        String tag = trim(raw).toLowerCase(Locale.ROOT);
        int length = tag.codePointCount(0, tag.length());
        if (length < 1 || length > 50) {
            throw new IllegalArgumentException("tag must be 1 to 50 characters");
        }
        return tag;
    }
}
