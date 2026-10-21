package com.mindspace.common;

public class RateLimitedException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitedException(long retryAfterSeconds) {
        super("Too many requests");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
