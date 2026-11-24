package dev.wakey.ratelimit.internal;

import dev.wakey.ratelimit.Decision;
import dev.wakey.ratelimit.RateLimit;
import dev.wakey.ratelimit.RateLimiter;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Token bucket per key, with a least-recently-used bound on the number of keys. */
final class TokenBucketRateLimiter implements RateLimiter {

    private static final double MILLIS_PER_SECOND = 1000.0;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int maxKeys;
    private final Clock clock;
    private final Object evictionLock = new Object();

    TokenBucketRateLimiter(int maxKeys, Clock clock) {
        if (maxKeys < 1) {
            throw new IllegalArgumentException("maxKeys must be at least 1");
        }
        this.maxKeys = maxKeys;
        this.clock = clock;
    }

    @Override
    public Decision tryAcquire(String key, RateLimit limit) {
        long nowMillis = clock.millis();
        Bucket bucket = bucketFor(key, limit, nowMillis);
        return bucket.tryAcquire(limit, nowMillis);
    }

    private Bucket bucketFor(String key, RateLimit limit, long nowMillis) {
        Bucket existing = buckets.get(key);
        if (existing != null) {
            return existing;
        }
        return addBucket(key, limit, nowMillis);
    }

    private Bucket addBucket(String key, RateLimit limit, long nowMillis) {
        synchronized (evictionLock) {
            Bucket existing = buckets.get(key);
            if (existing != null) {
                return existing;
            }
            if (buckets.size() >= maxKeys) {
                evictLeastRecentlyUsed();
            }
            Bucket created = new Bucket(limit.capacity(), nowMillis);
            buckets.put(key, created);
            return created;
        }
    }

    private void evictLeastRecentlyUsed() {
        String oldestKey = null;
        long oldestAccess = Long.MAX_VALUE;
        for (Map.Entry<String, Bucket> entry : buckets.entrySet()) {
            long access = entry.getValue().lastAccessMillis;
            if (access < oldestAccess) {
                oldestAccess = access;
                oldestKey = entry.getKey();
            }
        }
        if (oldestKey != null) {
            buckets.remove(oldestKey);
        }
    }

    private static final class Bucket {

        private double tokens;
        private long lastRefillMillis;
        private volatile long lastAccessMillis;

        private Bucket(int capacity, long nowMillis) {
            this.tokens = capacity;
            this.lastRefillMillis = nowMillis;
            this.lastAccessMillis = nowMillis;
        }

        private synchronized Decision tryAcquire(RateLimit limit, long nowMillis) {
            refill(limit, nowMillis);
            lastAccessMillis = nowMillis;
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return new Decision(true, Duration.ZERO);
            }
            long retryMillis = millisUntilNextToken(limit);
            return new Decision(false, Duration.ofMillis(retryMillis));
        }

        private void refill(RateLimit limit, long nowMillis) {
            long elapsedMillis = nowMillis - lastRefillMillis;
            if (elapsedMillis <= 0) {
                return;
            }
            double refilled = elapsedMillis * limit.refillPerSecond() / MILLIS_PER_SECOND;
            tokens = Math.min(limit.capacity(), tokens + refilled);
            lastRefillMillis = nowMillis;
        }

        private long millisUntilNextToken(RateLimit limit) {
            double secondsNeeded = (1.0 - tokens) / limit.refillPerSecond();
            return (long) Math.ceil(secondsNeeded * MILLIS_PER_SECOND);
        }
    }
}
