package dev.wakey.ratelimit.internal;

import static org.assertj.core.api.Assertions.assertThat;

import dev.wakey.ratelimit.Decision;
import dev.wakey.ratelimit.RateLimit;
import dev.wakey.ratelimit.RateLimiter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TokenBucketRateLimiterTest {

    private static final RateLimit LIMIT = new RateLimit(2, 0.5);

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    private final MutableClock clock = new MutableClock(START);
    private final RateLimiter limiter = new TokenBucketRateLimiter(10_000, clock);

    @Test
    void allowsUpToTheCapacity() {
        // when
        Decision first = limiter.tryAcquire("client", LIMIT);
        Decision second = limiter.tryAcquire("client", LIMIT);

        // then
        assertThat(first.allowed()).isTrue();
        assertThat(first.retryAfter()).isZero();
        assertThat(second.allowed()).isTrue();
    }

    @Test
    void deniesWithRetryAfterWhenEmpty() {
        // given
        limiter.tryAcquire("client", LIMIT);
        limiter.tryAcquire("client", LIMIT);

        // when
        Decision denied = limiter.tryAcquire("client", LIMIT);

        // then
        assertThat(denied.allowed()).isFalse();
        assertThat(denied.retryAfter()).isEqualTo(Duration.ofSeconds(2));
    }

    @Test
    void refillsOverTime() {
        // given
        limiter.tryAcquire("client", LIMIT);
        limiter.tryAcquire("client", LIMIT);

        // when
        clock.advance(Duration.ofSeconds(2));
        Decision afterRefill = limiter.tryAcquire("client", LIMIT);

        // then
        assertThat(afterRefill.allowed()).isTrue();
    }

    @Test
    void keysAreIndependent() {
        // given
        limiter.tryAcquire("client-a", LIMIT);
        limiter.tryAcquire("client-a", LIMIT);

        // when
        Decision otherClient = limiter.tryAcquire("client-b", LIMIT);
        Decision drainedClient = limiter.tryAcquire("client-a", LIMIT);

        // then
        assertThat(otherClient.allowed()).isTrue();
        assertThat(drainedClient.allowed()).isFalse();
    }

    @Test
    void idleKeysAreEvicted() {
        // given
        RateLimit slow = new RateLimit(2, 0.1);
        RateLimiter bounded = new TokenBucketRateLimiter(3, clock);
        bounded.tryAcquire("a", slow);
        clock.advance(Duration.ofSeconds(1));
        bounded.tryAcquire("b", slow);
        clock.advance(Duration.ofSeconds(1));
        bounded.tryAcquire("c", slow);
        clock.advance(Duration.ofSeconds(1));
        bounded.tryAcquire("a", slow);

        // when
        bounded.tryAcquire("d", slow);
        Decision evictedFirst = bounded.tryAcquire("b", slow);
        Decision evictedSecond = bounded.tryAcquire("b", slow);
        Decision surviving = bounded.tryAcquire("a", slow);

        // then
        assertThat(evictedFirst.allowed()).isTrue();
        assertThat(evictedSecond.allowed()).isTrue();
        assertThat(surviving.allowed()).isFalse();
    }

    @Test
    void evictionRemovesTheLeastRecentlyUsedKeyFirst() {
        // given
        RateLimit limit = new RateLimit(1, 1.0);
        RateLimiter bounded = new TokenBucketRateLimiter(2, clock);
        bounded.tryAcquire("first", limit);
        bounded.tryAcquire("second", limit);

        // when
        bounded.tryAcquire("newcomer", limit);

        // then the survivor is still drained, while the evicted key starts with a fresh bucket
        Decision surviving = bounded.tryAcquire("second", limit);
        Decision evicted = bounded.tryAcquire("first", limit);
        assertThat(surviving.allowed()).isFalse();
        assertThat(evicted.allowed()).isTrue();
    }

    @Test
    void aRecentlyUsedBucketSurvivesEvictionOfOlderOnes() {
        // given
        RateLimit limit = new RateLimit(1, 1.0);
        RateLimiter bounded = new TokenBucketRateLimiter(2, clock);
        bounded.tryAcquire("recent", limit);
        bounded.tryAcquire("idle", limit);
        bounded.tryAcquire("recent", limit);

        // when
        bounded.tryAcquire("newcomer", limit);

        // then the recently used bucket survives, while the idle one starts fresh
        Decision surviving = bounded.tryAcquire("recent", limit);
        Decision evicted = bounded.tryAcquire("idle", limit);
        assertThat(surviving.allowed()).isFalse();
        assertThat(evicted.allowed()).isTrue();
    }

    @Test
    void theRateLimiterEvictsInAmortizedConstantTime() {
        // given
        RateLimiter bounded = new TokenBucketRateLimiter(1_000, clock);
        RateLimit limit = new RateLimit(1, 1.0);
        bounded.tryAcquire("hot", limit);
        long startedNanos = System.nanoTime();

        // when
        for (int index = 0; index < 100_000; index++) {
            bounded.tryAcquire("key-" + index, limit);
        }
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedNanos);

        // then the many inserts finish without a linear scan per key
        assertThat(elapsed).isLessThan(Duration.ofSeconds(5));

        // and a cheap insert does not reset the capacity of an active key
        RateLimiter small = new TokenBucketRateLimiter(2, clock);
        small.tryAcquire("older", limit);
        small.tryAcquire("active", limit);
        Decision activeDenied = small.tryAcquire("active", limit);
        small.tryAcquire("newcomer", limit);
        Decision afterBypass = small.tryAcquire("active", limit);
        assertThat(activeDenied.allowed()).isFalse();
        assertThat(afterBypass.allowed()).isFalse();
    }

    @Test
    void concurrentCallsNeverExceedTheCapacity() throws Exception {
        // given
        int capacity = 100;
        RateLimit limit = new RateLimit(capacity, 0.000_001);
        Clock fixedClock = Clock.fixed(START, ZoneOffset.UTC);
        RateLimiter underTest = new TokenBucketRateLimiter(10_000, fixedClock);
        int threads = 16;
        int callsPerThread = 25;
        AtomicInteger allowed = new AtomicInteger();

        // when
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int thread = 0; thread < threads; thread++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    for (int call = 0; call < callsPerThread; call++) {
                        if (underTest.tryAcquire("hot", limit).allowed()) {
                            allowed.incrementAndGet();
                        }
                    }
                    return true;
                }));
            }
            start.countDown();
            for (Future<Boolean> future : futures) {
                assertThat(future.get()).isTrue();
            }
        }

        // then
        assertThat(allowed.get()).isEqualTo(capacity);
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
