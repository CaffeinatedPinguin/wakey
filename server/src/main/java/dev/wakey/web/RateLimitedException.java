package dev.wakey.web;

import java.time.Duration;

/** A rate limit was exceeded; the HTTP layer renders 429 with a {@code Retry-After} header. */
public final class RateLimitedException extends RuntimeException {

    private final Duration retryAfter;

    public RateLimitedException(Duration retryAfter) {
        super("rate_limited");
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
