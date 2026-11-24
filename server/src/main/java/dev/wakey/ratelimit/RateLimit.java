package dev.wakey.ratelimit;

/** A token bucket shape: a burst of {@code capacity} tokens refilled at {@code refillPerSecond}. */
public record RateLimit(int capacity, double refillPerSecond) {

    public RateLimit {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1");
        }
        if (!Double.isFinite(refillPerSecond) || refillPerSecond <= 0) {
            throw new IllegalArgumentException("refillPerSecond must be a positive finite number");
        }
    }
}
