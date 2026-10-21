package com.mindspace.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.UUID;

/**
 * Starts the whole application against a real PostgreSQL 16 (Flyway migrations + Hibernate validation).
 * Needs a Docker daemon. Run with {@code mvn verify} (failsafe picks up *IT classes).
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    public static final String PASSWORD = "synthetic-test-passphrase-42";

    /**
     * One container for the whole test JVM. Spring caches the application context across test classes, so a
     * per-class container (the @Container default) would be stopped while the cached context still points at it.
     * It is stopped by Testcontainers' Ryuk reaper when the JVM exits.
     */
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected JdbcTemplate jdbc;

    protected ApiClient anonymous() {
        return new ApiClient("http://localhost:" + port).bootstrapCsrf();
    }

    /** Registers a fresh synthetic user (signed in via cookies on the returned client). */
    protected ApiClient newUser(String name) {
        ApiClient c = anonymous();
        String email = name.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test";
        ApiClient.Reply r = c.post("/api/v1/auth/register", java.util.Map.of(
                "email", email, "password", PASSWORD, "confirmPassword", PASSWORD,
                "displayName", name, "timezone", "UTC", "acceptTerms", true));
        if (r.status() != 201 && r.status() != 200) {
            throw new IllegalStateException("register failed: " + r.status() + " " + r.raw());
        }
        return c;
    }

    @BeforeEach
    void noop() {
        // placeholder so subclasses may rely on the container being up
    }
}
