package dev.wakey.ratelimit;

import java.time.Duration;

/** The outcome of an acquire attempt; {@code retryAfter} is zero when the call is allowed. */
public record Decision(boolean allowed, Duration retryAfter) {

    public Decision {
        if (retryAfter.isNegative()) {
            throw new IllegalArgumentException("retryAfter must not be negative");
        }
        if (allowed && !retryAfter.isZero()) {
            throw new IllegalArgumentException("retryAfter must be zero when allowed");
        }
    }
}
