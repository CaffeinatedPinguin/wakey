package dev.wakey.ratelimit;

/** A small in-memory rate limiter; the caller maps a denied decision onto HTTP 429. */
public interface RateLimiter {

    Decision tryAcquire(String key, RateLimit limit);
}
