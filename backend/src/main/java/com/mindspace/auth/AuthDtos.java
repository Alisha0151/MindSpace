package com.mindspace.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 320) String email,
            @NotBlank @Size(max = 200) String password,
            @NotBlank @Size(max = 200) String confirmPassword,
            @NotBlank @Size(max = 100) String displayName,
            @NotBlank @Size(max = 64) String timezone,
            boolean acceptTerms) {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 320) String email,
            @NotBlank @Size(max = 200) String password) {
    }

    public record SessionResponse(UUID id, Instant issuedAt, Instant expiresAt, String userAgent, boolean current) {
    }
}
