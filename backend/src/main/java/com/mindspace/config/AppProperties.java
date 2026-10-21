package com.mindspace.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/** Single source of truth for application settings (bound from application.yml / environment). */
@Validated
@ConfigurationProperties(prefix = "mindspace")
public record AppProperties(@Valid @NotNull Security security,
                            @Valid @NotNull Cors cors,
                            @Valid @NotNull RateLimit rateLimit,
                            @Valid @NotNull Request request) {

    public record Security(@NotBlank @Size(min = 32, message = "jwt-secret must be at least 32 characters") String jwtSecret,
                           @NotNull Duration accessTokenTtl,
                           @NotNull Duration refreshTokenTtl,
                           @Min(4) @Max(16) int bcryptStrength,
                           boolean cookieSecure,
                           @NotBlank String cookieSameSite) {
    }

    public record Cors(@NotEmpty List<String> allowedOrigins) {
    }

    public record RateLimit(@Min(1) int authIpMax, @NotNull Duration authIpWindow,
                            @Min(1) int loginEmailMax, @NotNull Duration loginEmailWindow,
                            @Min(1) int sensitiveIpMax, @NotNull Duration sensitiveIpWindow) {
    }

    public record Request(@Min(1024) int maxBodyBytes) {
    }
}
