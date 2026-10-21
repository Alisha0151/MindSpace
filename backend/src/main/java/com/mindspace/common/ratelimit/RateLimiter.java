package com.mindspace.common.ratelimit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Sliding-window rate limiter: at most {@code maxEvents} per key within {@code windowNanos}.
 * In-memory and per-instance (see docs/SECURITY.md: a multi-instance deployment needs a shared store).
 */
public final class RateLimiter {

    private final int maxEvents;
    private final long windowNanos;
    private final LongSupplier nanoClock;
    private final ConcurrentHashMap<String, Deque<Long>> events = new ConcurrentHashMap<>();

    public RateLimiter(int maxEvents, java.time.Duration window) {
        this(maxEvents, window, System::nanoTime);
    }

    /** Visible for tests: inject a fake clock. */
    public RateLimiter(int maxEvents, java.time.Duration window, LongSupplier nanoClock) {
        if (maxEvents < 1) {
            throw new IllegalArgumentException("maxEvents must be at least 1");
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
        this.maxEvents = maxEvents;
        this.windowNanos = window.toNanos();
        this.nanoClock = nanoClock;
    }

    /** Records an event for {@code key} and returns true if it is within the limit. Rejected events are not recorded. */
    public boolean tryAcquire(String key) {
        long now = nanoClock.getAsLong();
        Deque<Long> deque = events.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            evictOld(deque, now);
            if (deque.size() >= maxEvents) {
                return false;
            }
            deque.addLast(now);
            return true;
        }
    }

    /** Seconds until the next event for {@code key} would be allowed (0 when it is allowed now). */
    public long retryAfterSeconds(String key) {
        Deque<Long> deque = events.get(key);
        if (deque == null) {
            return 0;
        }
        long now = nanoClock.getAsLong();
        synchronized (deque) {
            evictOld(deque, now);
            if (deque.size() < maxEvents) {
                return 0;
            }
            long waitNanos = windowNanos - (now - deque.peekFirst());
            return Math.max(1, (waitNanos + 999_999_999L) / 1_000_000_000L);
        }
    }

    /** Drops keys whose events have all expired so the map cannot grow without bound. */
    public void purgeIdle() {
        long now = nanoClock.getAsLong();
        events.entrySet().removeIf(entry -> {
            Deque<Long> deque = entry.getValue();
            synchronized (deque) {
                evictOld(deque, now);
                return deque.isEmpty();
            }
        });
    }

    int trackedKeys() {
        return events.size();
    }

    private void evictOld(Deque<Long> deque, long now) {
        while (!deque.isEmpty() && now - deque.peekFirst() >= windowNanos) {
            deque.pollFirst();
        }
    }
}
