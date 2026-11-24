package dev.wakey.retry;

import java.time.Duration;
import java.util.random.RandomGenerator;
import org.jspecify.annotations.Nullable;

/**
 * A stateless retry policy with an optional attempt limit and an optional time budget. The delay
 * grows exponentially from {@code baseDelay} up to {@code maxDelay} and gets a random jitter.
 */
public record RetryPolicy(
        @Nullable Integer maxAttempts,
        @Nullable Duration timeBudget,
        Duration baseDelay,
        double multiplier,
        Duration maxDelay,
        double jitterRatio) {

    public RetryPolicy {
        if (maxAttempts == null && timeBudget == null) {
            throw new IllegalArgumentException("At least one of maxAttempts or timeBudget must be set");
        }
        if (maxAttempts != null) {
            requireAtLeast(maxAttempts, 1.0, "maxAttempts");
        }
        if (timeBudget != null) {
            requirePositive(timeBudget, "timeBudget");
        }
        requirePositive(baseDelay, "baseDelay");
        requirePositive(maxDelay, "maxDelay");
        requireAtLeast(multiplier, 1.0, "multiplier");
        requireAtLeast(maxDelay, baseDelay, "maxDelay must not be shorter than baseDelay");
        if (jitterRatio < 0.0 || jitterRatio >= 1.0) {
            throw new IllegalArgumentException("jitterRatio must be in [0, 1)");
        }
    }

    /** Decides whether the next attempt may run, using {@code elapsed} since the first attempt. */
    public RetryDecision decide(int failedAttempts, Duration elapsed, RandomGenerator random) {
        RetryDecision decision;
        if (maxAttempts != null && failedAttempts >= maxAttempts) {
            decision = new RetryDecision.GiveUp(RetryDecision.GiveUp.Reason.MAX_ATTEMPTS);
        } else {
            Duration delay = nextDelay(failedAttempts, random);
            decision = decideForDelay(elapsed, delay);
        }
        return decision;
    }

    private RetryDecision decideForDelay(Duration elapsed, Duration delay) {
        Duration budget = timeBudget;
        RetryDecision decision = new RetryDecision.RetryAfter(delay);
        if (budget != null && exceedsBudget(elapsed, delay, budget)) {
            decision = new RetryDecision.GiveUp(RetryDecision.GiveUp.Reason.TIME_BUDGET);
        }
        return decision;
    }

    private boolean exceedsBudget(Duration elapsed, Duration delay, Duration budget) {
        Duration total = elapsed.plus(delay);
        return total.compareTo(budget) > 0;
    }

    private Duration nextDelay(int failedAttempts, RandomGenerator random) {
        long cappedNanos = cappedDelayNanos(failedAttempts);
        double variation = random.nextDouble() * 2.0 - 1.0;
        double jitteredNanos = cappedNanos * (1.0 + jitterRatio * variation);
        return Duration.ofNanos(Math.round(jitteredNanos));
    }

    private long cappedDelayNanos(int failedAttempts) {
        double exponent = failedAttempts - 1;
        double growth = Math.pow(multiplier, exponent);
        double uncappedNanos = baseDelay.toNanos() * growth;
        double cappedNanos = Math.min(maxDelay.toNanos(), uncappedNanos);
        return (long) cappedNanos;
    }

    private static void requirePositive(@Nullable Duration value, String name) {
        if (value == null || value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static void requireAtLeast(double value, double minimum, String name) {
        if (value < minimum) {
            throw new IllegalArgumentException(name + " must be at least " + minimum);
        }
    }

    private static void requireAtLeast(@Nullable Duration value, Duration minimum, String name) {
        if (value == null || value.compareTo(minimum) < 0) {
            throw new IllegalArgumentException(name);
        }
    }
}
