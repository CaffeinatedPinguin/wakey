package dev.wakey.checks.internal;

import dev.wakey.leadership.LeaderLease;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Consumes results only while this instance holds the consumer lease. The lease is renewed every
 * ten seconds; losing it stops consumption until it can be acquired again.
 */
@Component
@RequiredArgsConstructor
class CheckResultConsumerLoop {

    static final String LEASE_NAME = "check-results-consumer";

    private static final Duration RENEW_INTERVAL = Duration.ofSeconds(10);

    private final LeaderLease lease;

    private final CheckResultConsumer consumer;

    private final Clock clock;

    private boolean leader;

    @Nullable
    private Instant lastRenewal;

    boolean runOnce() {
        Instant now = clock.instant();
        if (!leader || lastRenewal == null || !now.isBefore(lastRenewal.plus(RENEW_INTERVAL))) {
            leader = lease.acquireOrRenew(LEASE_NAME).isPresent();
            lastRenewal = now;
        }
        if (!leader) {
            return false;
        }
        consumer.consumeBatch();
        return true;
    }
}
