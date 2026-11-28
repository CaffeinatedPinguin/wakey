package dev.wakey.checks.internal;

import dev.wakey.leadership.LeaderLease;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Schedules checks only while this instance holds the scheduler lease. The lease is renewed every
 * ten seconds; losing it stops planning until it can be acquired again.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class CheckSchedulerLoop {

    static final String LEASE_NAME = "check-scheduler";

    private static final Duration RENEW_INTERVAL = Duration.ofSeconds(10);

    private final LeaderLease lease;

    private final CheckScheduler scheduler;

    private final Clock clock;

    private boolean leader;

    @Nullable
    private Instant lastRenewal;

    boolean runOnce() {
        Instant now = clock.instant();
        if (renewalDue(now)) {
            renew(now);
        }
        if (!leader) {
            return false;
        }
        scheduler.runCycle();
        return true;
    }

    private boolean renewalDue(Instant now) {
        return !leader || lastRenewal == null || !now.isBefore(lastRenewal.plus(RENEW_INTERVAL));
    }

    private void renew(Instant now) {
        boolean wasLeader = leader;
        leader = lease.acquireOrRenew(LEASE_NAME).isPresent();
        lastRenewal = now;
        if (leader && !wasLeader) {
            log.info("Acquired the check scheduler lease {}", LEASE_NAME);
        } else if (!leader && wasLeader) {
            log.info("Lost the check scheduler lease {}", LEASE_NAME);
        }
    }
}
