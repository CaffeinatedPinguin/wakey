package dev.wakey.checks.internal;

import static org.assertj.core.api.Assertions.assertThat;

import dev.wakey.checks.Check;
import dev.wakey.checks.CheckActor;
import dev.wakey.checks.CheckDraft;
import dev.wakey.checks.CheckOutcome;
import dev.wakey.checks.CheckResult;
import dev.wakey.checks.CheckResults;
import dev.wakey.checks.CheckService;
import dev.wakey.checks.CheckState;
import dev.wakey.checks.CheckStatePolicy;
import dev.wakey.checks.CheckType;
import dev.wakey.checks.ErrorCategory;
import dev.wakey.leadership.LeaderLease;
import dev.wakey.leadership.LeaseGrant;
import dev.wakey.spaces.SpaceService;
import dev.wakey.testsupport.PostgresIntegrationTest;
import dev.wakey.web.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class CheckResultConsumerIntegrationTest extends PostgresIntegrationTest {

    private static final String ISSUER = "https://issuer.example.com";

    private static final CurrentUser ADMIN = new CurrentUser(ISSUER, "results-admin", true);

    private static final CheckActor ACTOR = new CheckActor("results-test");

    private CheckService checks;

    private CheckResults results;

    private CheckResultConsumer consumer;

    private UUID spaceId;

    @BeforeEach
    void setUp() {
        checks = bean(CheckService.class);
        results = bean(CheckResults.class);
        consumer = bean(CheckResultConsumer.class);
        SpaceService spaces = bean(SpaceService.class);
        spaceId = spaces.put(ADMIN, uniqueSlug("results"), "Results", null)
                .space()
                .id();
    }

    @Test
    void aPublishedResultIsStoredAndChangesTheState() {
        // given
        Check check = createCheck("published");
        UUID id = check.id();
        Instant checkedAt = instant("2026-01-01T00:00:00Z");

        // when
        publish(id, result(CheckState.UP, null, checkedAt));
        consumer.consumeBatch();

        // then
        Check read = checks.get(spaceId, id);
        CheckResult latest = results.latest(id).orElseThrow();
        List<IntervalRow> intervals = intervalRows(id);
        assertThat(read.state()).isEqualTo(CheckState.UP);
        assertThat(latest.state()).isEqualTo(CheckState.UP);
        assertThat(latest.locationId()).isEqualTo(CheckResult.SERVER_LOCATION);
        assertThat(intervals).hasSize(1);
    }

    @Test
    void aRepeatedMessageIsStoredOnce() {
        // given
        Check check = createCheck("repeated");
        UUID id = check.id();
        CheckResult result = result(CheckState.UP, null, instant("2026-01-01T00:00:00Z"));
        publish(id, result);
        publish(id, result);

        // when
        consumer.consumeBatch();

        // then
        Check read = checks.get(spaceId, id);
        List<IntervalRow> intervals = intervalRows(id);
        assertThat(resultCount(id)).isEqualTo(1L);
        assertThat(intervals).hasSize(1);
        assertThat(read.state()).isEqualTo(CheckState.UP);
    }

    @Test
    void aStateChangeClosesTheIntervalAndOpensANewOne() {
        // given
        Check check = createCheck("changed");
        Instant first = instant("2026-01-01T00:00:00Z");
        Instant second = instant("2026-01-01T00:00:10Z");

        // when
        publish(check.id(), result(CheckState.UP, null, first));
        consumer.consumeBatch();
        publish(check.id(), result(CheckState.DOWN, ErrorCategory.TARGET_DOWN, second));
        consumer.consumeBatch();

        // then
        List<IntervalRow> intervals = intervalRows(check.id());
        assertThat(intervals).hasSize(2);
        IntervalRow closed = intervals.get(0);
        IntervalRow open = intervals.get(1);
        assertThat(closed.state()).isEqualTo(CheckState.UP);
        assertThat(closed.startedAt()).isEqualTo(first);
        assertThat(closed.endedAt()).isEqualTo(second);
        assertThat(open.state()).isEqualTo(CheckState.DOWN);
        assertThat(open.startedAt()).isEqualTo(second);
        assertThat(open.endedAt()).isNull();
    }

    @Test
    void theSameStateKeepsTheOpenInterval() {
        // given
        Check check = createCheck("stable");
        UUID id = check.id();
        Instant first = instant("2026-01-01T00:00:00Z");
        Instant second = instant("2026-01-01T00:00:10Z");

        // when
        publish(id, result(CheckState.UP, null, first));
        consumer.consumeBatch();
        publish(id, result(CheckState.UP, null, second));
        consumer.consumeBatch();

        // then
        List<IntervalRow> intervals = intervalRows(id);
        assertThat(intervals).hasSize(1);
        IntervalRow open = intervals.getFirst();
        assertThat(open.state()).isEqualTo(CheckState.UP);
        assertThat(open.startedAt()).isEqualTo(first);
        assertThat(open.endedAt()).isNull();
        assertThat(resultCount(id)).isEqualTo(2L);
    }

    @Test
    void aLocationErrorIsNeverDown() {
        // given
        Check check = createCheck("location");
        UUID id = check.id();
        CheckResult locationError =
                result(CheckState.DOWN, ErrorCategory.LOCATION_NETWORK, instant("2026-01-01T00:00:00Z"));

        // when
        publish(id, locationError);
        consumer.consumeBatch();

        // then
        Check read = checks.get(spaceId, id);
        CheckResult latest = results.latest(id).orElseThrow();
        CheckState evaluatedState =
                CheckStatePolicy.evaluate(locationError, true).state();
        assertThat(evaluatedState).isEqualTo(CheckState.UNKNOWN);
        assertThat(latest.state()).isEqualTo(CheckState.UNKNOWN);
        assertThat(read.state()).isEqualTo(CheckState.UNKNOWN);
    }

    @Test
    void aCheckWithoutASuccessfulResultNeedsAConfigurationReview() {
        // given
        CheckResult locationError =
                result(CheckState.UNKNOWN, ErrorCategory.LOCATION_NETWORK, instant("2026-01-01T00:00:00Z"));
        CheckResult successful = result(CheckState.UP, null, instant("2026-01-01T00:00:10Z"));

        // when
        CheckOutcome beforeFirstSuccess = CheckStatePolicy.evaluate(locationError, false);
        CheckOutcome afterFirstSuccess = CheckStatePolicy.evaluate(successful, true);

        // then
        assertThat(beforeFirstSuccess.state()).isEqualTo(CheckState.UNKNOWN);
        assertThat(beforeFirstSuccess.needsConfigurationReview()).isTrue();
        assertThat(afterFirstSuccess.state()).isEqualTo(CheckState.UP);
        assertThat(afterFirstSuccess.needsConfigurationReview()).isFalse();
    }

    @Test
    void onlyTheLeaderConsumesResults() {
        // given
        Check check = createCheck("leader");
        UUID id = check.id();
        Instant checkedAt = instant("2026-01-01T00:00:00Z");
        CheckResult result = result(CheckState.UP, null, checkedAt);
        publish(id, result);
        Clock clock = bean(Clock.class);
        Optional<LeaseGrant> noGrant = Optional.empty();
        LeaderLease followerLease = lease(noGrant);
        LeaderLease leaderLease = bean(LeaderLease.class);
        CheckResultConsumerLoop follower = new CheckResultConsumerLoop(followerLease, consumer, clock);
        CheckResultConsumerLoop leader = new CheckResultConsumerLoop(leaderLease, consumer, clock);

        // when
        boolean followerConsumed = follower.runOnce();
        Check afterFollower = checks.get(spaceId, id);
        boolean leaderConsumed = leader.runOnce();
        Check afterLeader = checks.get(spaceId, id);

        // then
        assertThat(followerConsumed).isFalse();
        assertThat(afterFollower.state()).isEqualTo(CheckState.UNKNOWN);
        assertThat(leaderConsumed).isTrue();
        assertThat(afterLeader.state()).isEqualTo(CheckState.UP);
        assertThat(resultCount(id)).isEqualTo(1L);
    }

    private void publish(UUID checkId, CheckResult result) {
        TransactionTemplate template = new TransactionTemplate(bean(PlatformTransactionManager.class));
        template.executeWithoutResult(status -> results.publish(checkId, spaceId, result));
    }

    private long resultCount(UUID checkId) {
        return jdbc().sql("SELECT COUNT(*) FROM check_result WHERE check_id = :checkId")
                .param("checkId", checkId)
                .query(Long.class)
                .single();
    }

    private List<IntervalRow> intervalRows(UUID checkId) {
        return jdbc().sql("""
                        SELECT state, started_at, ended_at
                        FROM check_state_interval
                        WHERE check_id = :checkId
                        ORDER BY started_at, ended_at NULLS LAST
                        """)
                .param("checkId", checkId)
                .query((rs, rowNum) -> {
                    String state = rs.getString("state");
                    OffsetDateTime startedAt = rs.getObject("started_at", OffsetDateTime.class);
                    OffsetDateTime endedAt = rs.getObject("ended_at", OffsetDateTime.class);
                    CheckState parsedState = CheckState.valueOf(state);
                    return new IntervalRow(parsedState, startedAt.toInstant(), toInstant(endedAt));
                })
                .list();
    }

    private Check createCheck(String name) {
        CheckDraft draft = new CheckDraft(
                name, CheckType.HTTP, "https://" + name + ".test", 60, 10, 1, null, null, null, List.of());
        return checks.create(spaceId, draft, ACTOR);
    }

    private static @Nullable Instant toInstant(@Nullable OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }

    private static CheckResult result(CheckState state, @Nullable ErrorCategory category, Instant checkedAt) {
        return new CheckResult(null, checkedAt, state, category, null, null, null, null);
    }

    private static LeaderLease lease(Optional<LeaseGrant> grant) {
        return new LeaderLease() {
            @Override
            public Optional<LeaseGrant> acquireOrRenew(String name) {
                return grant;
            }

            @Override
            public void release(String name) {}
        };
    }

    private static Instant instant(String value) {
        return Instant.parse(value);
    }

    private static String uniqueSlug(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private record IntervalRow(
            CheckState state, Instant startedAt, @Nullable Instant endedAt) {}
}
