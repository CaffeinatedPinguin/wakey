package dev.wakey.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class RetryPolicyTest {

    @Test
    void delayGrowsExponentiallyUpToTheMaximum() {
        // given
        RetryPolicy policy = new RetryPolicy(10, null, Duration.ofSeconds(1), 2.0, Duration.ofSeconds(8), 0.0);
        RandomGenerator random = new Random(1L);

        // when
        List<Duration> delays = new ArrayList<>();
        for (int failedAttempts = 1; failedAttempts <= 5; failedAttempts++) {
            RetryDecision decision = policy.decide(failedAttempts, Duration.ZERO, random);
            RetryDecision.RetryAfter retry = (RetryDecision.RetryAfter) decision;
            delays.add(retry.delay());
        }

        // then
        assertThat(delays).isEqualTo(seconds(1, 2, 4, 8, 8));
    }

    @Test
    void jitterStaysWithinTheRatio() {
        // given
        RetryPolicy policy = new RetryPolicy(100, null, Duration.ofSeconds(10), 2.0, Duration.ofMinutes(1), 0.5);
        RandomGenerator random = new Random(42L);
        Duration lower = Duration.ofSeconds(5);
        Duration upper = Duration.ofSeconds(15);

        // when
        List<Duration> delays = new ArrayList<>();
        for (int draw = 0; draw < 200; draw++) {
            RetryDecision decision = policy.decide(1, Duration.ZERO, random);
            RetryDecision.RetryAfter retry = (RetryDecision.RetryAfter) decision;
            delays.add(retry.delay());
        }

        // then
        assertThat(delays).allSatisfy(delay -> assertThat(delay).isBetween(lower, upper));
    }

    @Test
    void retryDelayNeverExceedsTheMaximum() {
        // given
        RetryPolicy policy = new RetryPolicy(100, null, Duration.ofSeconds(8), 2.0, Duration.ofSeconds(8), 0.5);
        RandomGenerator random = new Random(11L);
        Duration maximum = Duration.ofSeconds(8);

        // when
        List<Duration> delays = new ArrayList<>();
        for (int failedAttempts = 1; failedAttempts <= 50; failedAttempts++) {
            RetryDecision decision = policy.decide(failedAttempts, Duration.ZERO, random);
            RetryDecision.RetryAfter retry = (RetryDecision.RetryAfter) decision;
            delays.add(retry.delay());
        }

        // then
        assertThat(delays).allSatisfy(delay -> assertThat(delay).isLessThanOrEqualTo(maximum));
    }

    @Test
    void givesUpAfterMaxAttempts() {
        // given
        RetryPolicy policy = new RetryPolicy(3, null, Duration.ofSeconds(1), 2.0, Duration.ofSeconds(8), 0.0);
        RandomGenerator random = new Random(7L);

        // when
        RetryDecision decision = policy.decide(3, Duration.ZERO, random);

        // then
        assertThat(decision).isInstanceOf(RetryDecision.GiveUp.class);
        RetryDecision.GiveUp giveUp = (RetryDecision.GiveUp) decision;
        assertThat(giveUp.reason()).isEqualTo(RetryDecision.GiveUp.Reason.MAX_ATTEMPTS);
    }

    @Test
    void givesUpWhenTheTimeBudgetIsSpent() {
        // given
        RetryPolicy policy =
                new RetryPolicy(null, Duration.ofSeconds(5), Duration.ofSeconds(1), 2.0, Duration.ofSeconds(4), 0.0);
        RandomGenerator random = new Random(3L);
        Duration elapsed = Duration.ofSeconds(4);

        // when
        RetryDecision decision = policy.decide(2, elapsed, random);

        // then
        assertThat(decision).isInstanceOf(RetryDecision.GiveUp.class);
        RetryDecision.GiveUp giveUp = (RetryDecision.GiveUp) decision;
        assertThat(giveUp.reason()).isEqualTo(RetryDecision.GiveUp.Reason.TIME_BUDGET);
    }

    @Test
    void finalStateIsReportedOnce() {
        // given
        RetryPolicy policy = new RetryPolicy(2, null, Duration.ofSeconds(1), 2.0, Duration.ofSeconds(8), 0.0);
        RandomGenerator random = new Random(5L);

        // when
        RetryDecision first = policy.decide(2, Duration.ZERO, random);
        RetryDecision second = policy.decide(2, Duration.ZERO, random);

        // then
        assertThat(second).isEqualTo(first);
        assertThat(second).isInstanceOf(RetryDecision.GiveUp.class);
        RetryDecision.GiveUp giveUp = (RetryDecision.GiveUp) second;
        assertThat(giveUp.reason()).isEqualTo(RetryDecision.GiveUp.Reason.MAX_ATTEMPTS);
    }

    @Test
    void sameSeedGivesTheSameDelays() {
        // given
        RetryPolicy policy = new RetryPolicy(20, null, Duration.ofSeconds(1), 2.0, Duration.ofMinutes(1), 0.25);
        RandomGenerator firstRandom = new Random(99L);
        RandomGenerator secondRandom = new Random(99L);

        // when
        List<Duration> firstDelays = delaysFrom(policy, firstRandom);
        List<Duration> secondDelays = delaysFrom(policy, secondRandom);

        // then
        assertThat(secondDelays).isEqualTo(firstDelays);
    }

    @Test
    void invalidParametersAreRejected() {
        // when
        List<Executable> invalidPolicies = List.of(
                () -> new RetryPolicy(null, null, Duration.ofSeconds(1), 2.0, Duration.ofSeconds(8), 0.0),
                () -> new RetryPolicy(0, null, Duration.ofSeconds(1), 2.0, Duration.ofSeconds(8), 0.0),
                () -> new RetryPolicy(null, Duration.ZERO, Duration.ofSeconds(1), 2.0, Duration.ofSeconds(8), 0.0),
                () -> new RetryPolicy(5, null, Duration.ZERO, 2.0, Duration.ofSeconds(8), 0.0),
                () -> new RetryPolicy(5, null, Duration.ofSeconds(1), 0.5, Duration.ofSeconds(8), 0.0),
                () -> new RetryPolicy(5, null, Duration.ofSeconds(8), 2.0, Duration.ofSeconds(1), 0.0),
                () -> new RetryPolicy(5, null, Duration.ofSeconds(1), 2.0, Duration.ofSeconds(8), 1.0));

        // then
        assertThat(invalidPolicies).allSatisfy(RetryPolicyTest::assertRejected);
    }

    private static List<Duration> delaysFrom(RetryPolicy policy, RandomGenerator random) {
        List<Duration> delays = new ArrayList<>();
        for (int failedAttempts = 1; failedAttempts <= 10; failedAttempts++) {
            RetryDecision decision = policy.decide(failedAttempts, Duration.ZERO, random);
            RetryDecision.RetryAfter retry = (RetryDecision.RetryAfter) decision;
            delays.add(retry.delay());
        }
        return delays;
    }

    private static List<Duration> seconds(long... values) {
        List<Duration> durations = new ArrayList<>();
        for (long value : values) {
            durations.add(Duration.ofSeconds(value));
        }
        return durations;
    }

    private static void assertRejected(Executable executable) {
        assertThatThrownBy(executable::execute).isInstanceOf(IllegalArgumentException.class);
    }
}
