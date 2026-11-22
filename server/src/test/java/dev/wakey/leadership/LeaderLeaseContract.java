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
 * base class, for example {@code PostgresLeaderLeaseTest extends PostgresIntegrationTest implements
 * LeaderLeaseContract}.
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
        LeaseGrant grant = lease("holder-a", TTL).acquireOrRenew("checks").orElseThrow();

        assertThat(grant.name()).isEqualTo("checks");
        assertThat(grant.holderId()).isEqualTo("holder-a");
        assertThat(grant.fencingToken()).isEqualTo(1L);
        assertThat(grant.expiresAt()).isNotNull();
    }

    @Test
    default void renewingOwnLeaseKeepsTokenAndExtendsExpiry() {
        LeaderLease lease = lease("holder-a", TTL);
        LeaseGrant first = lease.acquireOrRenew("checks").orElseThrow();
        LeaseGrant renewed = lease.acquireOrRenew("checks").orElseThrow();

        assertThat(renewed.holderId()).isEqualTo(first.holderId());
        assertThat(renewed.fencingToken()).isEqualTo(first.fencingToken());
        assertThat(renewed.expiresAt()).isAfterOrEqualTo(first.expiresAt());
    }

    @Test
    default void anotherHolderCannotTakeLiveLease() {
        assertThat(lease("holder-a", TTL).acquireOrRenew("checks")).isPresent();
        assertThat(lease("holder-b", TTL).acquireOrRenew("checks")).isEmpty();
    }

    @Test
    default void expiredLeaseIsTakenOverWithHigherToken() throws InterruptedException {
        LeaseGrant first = lease("holder-a", Duration.ofMillis(100))
                .acquireOrRenew("checks")
                .orElseThrow();

        Thread.sleep(300);

        LeaseGrant second = lease("holder-b", TTL).acquireOrRenew("checks").orElseThrow();
        assertThat(second.holderId()).isEqualTo("holder-b");
        assertThat(second.fencingToken()).isEqualTo(first.fencingToken() + 1);
    }

    @Test
    default void releaseByOwnerFreesTheLeaseForAnotherHolder() {
        LeaderLease owner = lease("holder-a", TTL);
        LeaseGrant first = owner.acquireOrRenew("checks").orElseThrow();

        owner.release("checks");

        LeaseGrant second = lease("holder-b", TTL).acquireOrRenew("checks").orElseThrow();
        assertThat(second.holderId()).isEqualTo("holder-b");
        assertThat(second.fencingToken()).isEqualTo(first.fencingToken() + 1);
    }

    @Test
    default void releaseByAnotherHolderDoesNothing() {
        LeaderLease owner = lease("holder-a", TTL);
        LeaseGrant first = owner.acquireOrRenew("checks").orElseThrow();

        lease("holder-b", TTL).release("checks");

        assertThat(lease("holder-b", TTL).acquireOrRenew("checks")).isEmpty();
        assertThat(owner.acquireOrRenew("checks").orElseThrow().fencingToken()).isEqualTo(first.fencingToken());
    }

    @Test
    default void leasesWithDifferentNamesAreIndependent() {
        assertThat(lease("holder-a", TTL).acquireOrRenew("checks")).isPresent();
        assertThat(lease("holder-b", TTL).acquireOrRenew("alerts")).isPresent();
        assertThat(lease("holder-b", TTL).acquireOrRenew("checks")).isEmpty();
    }

    @Test
    default void exactlyOneOfEightConcurrentHoldersWins() throws Exception {
        List<Callable<Optional<LeaseGrant>>> attempts = new ArrayList<>();
        for (int index = 0; index < HOLDERS; index++) {
            LeaderLease lease = lease("holder-" + index, TTL);
            attempts.add(() -> lease.acquireOrRenew("checks"));
        }

        List<Optional<LeaseGrant>> results = runConcurrently(attempts);

        assertThat(results.stream().filter(Optional::isPresent).count()).isEqualTo(1L);
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
