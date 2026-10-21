package com.mindspace.common;

/** One validation problem, safe to return to clients (never contains submitted free text). */
public record FieldViolation(String field, String message) {
}
