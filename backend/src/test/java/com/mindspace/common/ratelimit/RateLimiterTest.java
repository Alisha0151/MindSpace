package com.mindspace.common.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    private static final long SECOND = 1_000_000_000L;

    @Test
    @DisplayName("allows up to the limit, then rejects")
    void limit() {
        AtomicLong clock = new AtomicLong();
        RateLimiter limiter = new RateLimiter(3, Duration.ofSeconds(60), clock::get);
        assertTrue(limiter.tryAcquire("ip"));
        assertTrue(limiter.tryAcquire("ip"));
        assertTrue(limiter.tryAcquire("ip"));
        assertFalse(limiter.tryAcquire("ip"));
    }

    @Test
    @DisplayName("keys are independent")
    void independentKeys() {
        AtomicLong clock = new AtomicLong();
        RateLimiter limiter = new RateLimiter(1, Duration.ofSeconds(60), clock::get);
        assertTrue(limiter.tryAcquire("a"));
        assertFalse(limiter.tryAcquire("a"));
        assertTrue(limiter.tryAcquire("b"));
    }

    @Test
    @DisplayName("events expire after the window")
    void windowSlides() {
        AtomicLong clock = new AtomicLong();
        RateLimiter limiter = new RateLimiter(2, Duration.ofSeconds(60), clock::get);
        assertTrue(limiter.tryAcquire("k"));       // t=0
        clock.set(30 * SECOND);
        assertTrue(limiter.tryAcquire("k"));       // t=30
        assertFalse(limiter.tryAcquire("k"));      // full
        clock.set(59 * SECOND);
        assertFalse(limiter.tryAcquire("k"));      // t=0 event still inside the window
        clock.set(60 * SECOND);
        assertTrue(limiter.tryAcquire("k"));       // t=0 event expired
        assertFalse(limiter.tryAcquire("k"));      // t=30 and t=60 are in the window
    }

    @Test
    @DisplayName("rejected attempts are not recorded, so they do not extend the lockout")
    void rejectionsNotRecorded() {
        AtomicLong clock = new AtomicLong();
        RateLimiter limiter = new RateLimiter(1, Duration.ofSeconds(10), clock::get);
        assertTrue(limiter.tryAcquire("k"));
        clock.set(5 * SECOND);
        assertFalse(limiter.tryAcquire("k"));
        clock.set(10 * SECOND);
        assertTrue(limiter.tryAcquire("k"));
    }

    @Test
    @DisplayName("retry-after reports the remaining wait in whole seconds, rounded up")
    void retryAfter() {
        AtomicLong clock = new AtomicLong();
        RateLimiter limiter = new RateLimiter(1, Duration.ofSeconds(60), clock::get);
        assertEquals(0, limiter.retryAfterSeconds("k"));
        assertTrue(limiter.tryAcquire("k"));
        assertEquals(60, limiter.retryAfterSeconds("k"));
        clock.set(45 * SECOND + 1);
        assertEquals(15, limiter.retryAfterSeconds("k"));
        clock.set(60 * SECOND);
        assertEquals(0, limiter.retryAfterSeconds("k"));
    }

    @Test
    @DisplayName("purgeIdle removes keys with only expired events")
    void purge() {
        AtomicLong clock = new AtomicLong();
        RateLimiter limiter = new RateLimiter(5, Duration.ofSeconds(60), clock::get);
        limiter.tryAcquire("old");
        clock.set(61 * SECOND);
        limiter.tryAcquire("new");
        limiter.purgeIdle();
        assertEquals(1, limiter.trackedKeys());
    }

    @Test
    @DisplayName("invalid configuration is rejected")
    void invalidConfig() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(0, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(1, Duration.ZERO));
    }
}
