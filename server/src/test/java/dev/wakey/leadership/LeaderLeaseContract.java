package dev.wakey.leadership;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Contract that every {@link LeaderLease} implementation must satisfy, regardless of the database.
 *
 * <p>This is a JUnit 5 test interface: its {@link Test @Test} and {@link BeforeEach @BeforeEach}
 * default methods run for every class that implements it. Concrete suites only choose a database
 * base class.
 */
public interface LeaderLeaseContract {

    Duration TTL = Duration.ofSeconds(30);

    int HOLDERS = 8;

    JdbcClient jdbc();

    @BeforeEach
    default void deleteLeases() {
        jdbc().sql("DELETE FROM leader_lock").update();
    }

    @Test
    default void firstAcquireTakesTheLeaseWithTokenOne() {
        // when
        LeaseGrant grant = lease("holder-a", TTL).acquireOrRenew("checks").orElseThrow();

        // then
        assertThat(grant.name()).isEqualTo("checks");
        assertThat(grant.holderId()).isEqualTo("holder-a");
        assertThat(grant.fencingToken()).isEqualTo(1L);
        assertThat(grant.expiresAt()).isNotNull();
    }

    @Test
    default void renewingOwnLeaseKeepsTokenAndExtendsExpiry() {
        // given
        LeaderLease lease = lease("holder-a", TTL);
        LeaseGrant first = lease.acquireOrRenew("checks").orElseThrow();

        // when
        LeaseGrant renewed = lease.acquireOrRenew("checks").orElseThrow();

        // then
        assertThat(renewed.holderId()).isEqualTo(first.holderId());
        assertThat(renewed.fencingToken()).isEqualTo(first.fencingToken());
        assertThat(renewed.expiresAt()).isAfterOrEqualTo(first.expiresAt());
    }

    @Test
    default void anotherHolderCannotTakeLiveLease() {
        // when
        Optional<LeaseGrant> firstAcquire = lease("holder-a", TTL).acquireOrRenew("checks");
        Optional<LeaseGrant> secondAcquire = lease("holder-b", TTL).acquireOrRenew("checks");

        // then
        assertThat(firstAcquire).isPresent();
        assertThat(secondAcquire).isEmpty();
    }

    @Test
    default void expiredLeaseIsTakenOverWithHigherToken() throws InterruptedException {
        // given
        LeaseGrant first = lease("holder-a", Duration.ofMillis(100))
                .acquireOrRenew("checks")
                .orElseThrow();
        Thread.sleep(300);

        // when
        LeaseGrant second = lease("holder-b", TTL).acquireOrRenew("checks").orElseThrow();

        // then
        assertThat(second.holderId()).isEqualTo("holder-b");
        assertThat(second.fencingToken()).isEqualTo(first.fencingToken() + 1);
    }

    @Test
    default void releaseByOwnerFreesTheLeaseForAnotherHolder() {
        // given
        LeaderLease owner = lease("holder-a", TTL);
        LeaseGrant first = owner.acquireOrRenew("checks").orElseThrow();

        // when
        owner.release("checks");
        LeaseGrant second = lease("holder-b", TTL).acquireOrRenew("checks").orElseThrow();

        // then
        assertThat(second.holderId()).isEqualTo("holder-b");
        assertThat(second.fencingToken()).isEqualTo(first.fencingToken() + 1);
    }

    @Test
    default void releaseByAnotherHolderDoesNothing() {
        // given
        LeaderLease owner = lease("holder-a", TTL);
        LeaseGrant first = owner.acquireOrRenew("checks").orElseThrow();

        // when
        lease("holder-b", TTL).release("checks");

        // then
        Optional<LeaseGrant> otherAcquire = lease("holder-b", TTL).acquireOrRenew("checks");
        LeaseGrant ownerRenewal = owner.acquireOrRenew("checks").orElseThrow();
        assertThat(otherAcquire).isEmpty();
        assertThat(ownerRenewal.fencingToken()).isEqualTo(first.fencingToken());
    }

    @Test
    default void leasesWithDifferentNamesAreIndependent() {
        // when
        Optional<LeaseGrant> checksForA = lease("holder-a", TTL).acquireOrRenew("checks");
        Optional<LeaseGrant> alertsForB = lease("holder-b", TTL).acquireOrRenew("alerts");
        Optional<LeaseGrant> checksForB = lease("holder-b", TTL).acquireOrRenew("checks");

        // then
        assertThat(checksForA).isPresent();
        assertThat(alertsForB).isPresent();
        assertThat(checksForB).isEmpty();
    }

    @Test
    default void exactlyOneOfEightConcurrentHoldersWins() throws Exception {
        // given
        List<Callable<Optional<LeaseGrant>>> attempts = new ArrayList<>();
        for (int index = 0; index < HOLDERS; index++) {
            LeaderLease lease = lease("holder-" + index, TTL);
            attempts.add(() -> lease.acquireOrRenew("checks"));
        }

        // when
        List<Optional<LeaseGrant>> results = runConcurrently(attempts);
        long winners = results.stream().filter(Optional::isPresent).count();

        // then
        assertThat(winners).isEqualTo(1L);
    }

    private JdbcLeaderLease lease(String holderId, Duration ttl) {
        return new JdbcLeaderLease(jdbc(), holderId, ttl);
    }

    private static List<Optional<LeaseGrant>> runConcurrently(List<Callable<Optional<LeaseGrant>>> attempts)
            throws InterruptedException, ExecutionException, TimeoutException {
        try (ExecutorService executor = Executors.newFixedThreadPool(attempts.size())) {
            CountDownLatch ready = new CountDownLatch(attempts.size());
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Optional<LeaseGrant>>> futures = new ArrayList<>();
            for (Callable<Optional<LeaseGrant>> attempt : attempts) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return attempt.call();
                }));
            }
            assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Optional<LeaseGrant>> results = new ArrayList<>();
            for (Future<Optional<LeaseGrant>> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        }
    }
}
