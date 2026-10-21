package com.mindspace.common;

import java.util.List;

public class ValidationException extends RuntimeException {

    private final List<FieldViolation> violations;

    public ValidationException(List<FieldViolation> violations) {
        super("Validation failed");
        this.violations = List.copyOf(violations);
    }

    public ValidationException(String field, String message) {
        this(List.of(new FieldViolation(field, message)));
    }

    public List<FieldViolation> violations() {
        return violations;
    }

    public static void throwIfAny(List<FieldViolation> violations) {
        if (!violations.isEmpty()) {
            throw new ValidationException(violations);
        }
    }
}
