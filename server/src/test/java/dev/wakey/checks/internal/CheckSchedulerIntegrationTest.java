package dev.wakey.checks.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import dev.wakey.checks.Check;
import dev.wakey.checks.CheckActor;
import dev.wakey.checks.CheckDraft;
import dev.wakey.checks.CheckExecutor;
import dev.wakey.checks.CheckExecutorsFixture;
import dev.wakey.checks.CheckFormDescriptor;
import dev.wakey.checks.CheckResult;
import dev.wakey.checks.CheckResults;
import dev.wakey.checks.CheckService;
import dev.wakey.checks.CheckState;
import dev.wakey.checks.CheckType;
import dev.wakey.checks.PausedReason;
import dev.wakey.leadership.LeaderLease;
import dev.wakey.leadership.LeaseGrant;
import dev.wakey.spaces.Space;
import dev.wakey.spaces.SpaceMember;
import dev.wakey.spaces.SpaceMembers;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceService;
import dev.wakey.testsupport.SecurityIntegrationTest;
import dev.wakey.testsupport.TestClock;
import dev.wakey.web.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class CheckSchedulerIntegrationTest extends SecurityIntegrationTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static final int PARALLELISM = 2;

    private static final String ISSUER = "https://issuer.example.com";

    private static final String SPACES_PATH = "/api/v1/spaces";

    private static final CurrentUser FIXTURE_ADMIN = new CurrentUser(ISSUER, "scheduler-admin", true);

    private static final CheckActor ACTOR = new CheckActor("scheduler-test");

    private TestClock clock;

    private CheckService checks;

    private CheckResults results;

    private CheckResultConsumer consumer;

    private CheckStore store;

    private PlatformTransactionManager transactions;

    private CheckSchedulerTasks tasks;

    private String subject;

    private String slug;

    private Space space;

    @BeforeEach
    void setUp() {
        clock = bean(TestClock.class);
        clock.clearPin();
        checks = bean(CheckService.class);
        results = bean(CheckResults.class);
        consumer = bean(CheckResultConsumer.class);
        transactions = bean(PlatformTransactionManager.class);
        tasks = new CheckSchedulerTasks(PARALLELISM);
        SpaceService spaces = bean(SpaceService.class);
        subject = uniqueSubject("scheduler");
        slug = uniqueSlug("scheduler");
        space = spaces.put(FIXTURE_ADMIN, slug, "Scheduler", null).space();
        bean(SpaceMembers.class).addMember(FIXTURE_ADMIN, slug, new SpaceMember(ISSUER, subject, SpaceRole.EDITOR));
        store = scopedStore(space.id());
    }

    @AfterEach
    void tearDown() {
        clock.clearPin();
        tasks.close();
    }

    @Test
    void aDueCheckIsExecutedAndItsResultPublished() {
        // given
        Check check = createCheck("due", 60);
        RecordingExecutor executor = new RecordingExecutor(clock);
        CheckScheduler scheduler = scheduler(executor);
        clock.pinAt(dueAt(check));

        // when
        scheduler.runCycle();
        consumer.consumeBatch();

        // then
        Check stored = checks.get(space.id(), check.id());
        Optional<CheckResult> latest = results.latest(check.id());
        long storedResults = resultCount(check.id());
        assertThat(executor.calls()).isEqualTo(1);
        assertThat(stored.state()).isEqualTo(CheckState.UP);
        assertThat(latest).isPresent();
        assertThat(storedResults).isEqualTo(1L);
    }

    @Test
    void aPausedCheckIsNotExecuted() {
        // given
        Check check = createCheck("paused", 60);
        checks.pause(space.id(), check.id(), PausedReason.MANUAL, ACTOR);
        RecordingExecutor executor = new RecordingExecutor(clock);
        CheckScheduler scheduler = scheduler(executor);
        clock.pinAt(dueAt(check));

        // when
        scheduler.runCycle();
        consumer.consumeBatch();

        // then
        Optional<CheckResult> latest = results.latest(check.id());
        long storedResults = resultCount(check.id());
        assertThat(executor.calls()).isZero();
        assertThat(latest).isEmpty();
        assertThat(storedResults).isZero();
    }

    @Test
    void aCheckIsNotExecutedBeforeItsInterval() {
        // given
        Check check = createCheck("early", 60);
        RecordingExecutor executor = new RecordingExecutor(clock);
        CheckScheduler scheduler = scheduler(executor);
        Instant tooEarly = check.createdAt().plusSeconds(check.intervalSeconds() - 1);
        clock.pinAt(tooEarly);

        // when
        scheduler.runCycle();
        int callsBeforeDue = executor.calls();
        clock.pinAt(dueAt(check));
        scheduler.runCycle();
        consumer.consumeBatch();

        // then
        Optional<CheckResult> latest = results.latest(check.id());
        assertThat(callsBeforeDue).isZero();
        assertThat(executor.calls()).isEqualTo(1);
        assertThat(latest).isPresent();
    }

    @Test
    void onlyTheLeaderSchedules() {
        // given
        Check check = createCheck("leader", 60);
        RecordingExecutor executor = new RecordingExecutor(clock);
        CheckScheduler scheduler = scheduler(executor);
        clock.pinAt(dueAt(check));
        LeaderLease followerLease = lease(Optional.empty());
        LeaderLease leaderLease = lease(grant());
        CheckSchedulerLoop follower = new CheckSchedulerLoop(followerLease, scheduler, clock);
        CheckSchedulerLoop leader = new CheckSchedulerLoop(leaderLease, scheduler, clock);

        // when
        boolean followerScheduled = follower.runOnce();
        int callsAfterFollower = executor.calls();
        boolean leaderScheduled = leader.runOnce();
        int callsAfterLeader = executor.calls();

        // then
        assertThat(followerScheduled).isFalse();
        assertThat(callsAfterFollower).isZero();
        assertThat(leaderScheduled).isTrue();
        assertThat(callsAfterLeader).isEqualTo(1);
    }

    @Test
    void aCheckDeletedDuringExecutionPublishesNothing() {
        // given
        Check check = createCheck("deleted", 60);
        RecordingExecutor executor =
                new RecordingExecutor(clock, executed -> checks.delete(executed.spaceId(), executed.id(), ACTOR));
        CheckScheduler scheduler = scheduler(executor);
        clock.pinAt(dueAt(check));

        // when
        scheduler.runCycle();
        consumer.consumeBatch();

        // then
        Optional<CheckResult> latest = results.latest(check.id());
        long storedResults = resultCount(check.id());
        assertThat(executor.calls()).isEqualTo(1);
        assertThat(latest).isEmpty();
        assertThat(storedResults).isZero();
    }

    @Test
    void aCheckPausedDuringExecutionPublishesNothing() {
        // given
        Check check = createCheck("paused-during", 60);
        RecordingExecutor executor = new RecordingExecutor(
                clock, executed -> checks.pause(executed.spaceId(), executed.id(), PausedReason.MANUAL, ACTOR));
        CheckScheduler scheduler = scheduler(executor);
        clock.pinAt(dueAt(check));

        // when
        scheduler.runCycle();
        consumer.consumeBatch();

        // then
        Optional<CheckResult> latest = results.latest(check.id());
        long storedResults = resultCount(check.id());
        assertThat(executor.calls()).isEqualTo(1);
        assertThat(latest).isEmpty();
        assertThat(storedResults).isZero();
    }

    @Test
    void aCheckAddedThroughTheApiGetsItsStateFromTheScheduler() throws Exception {
        // given
        try (HttpTestServer server = new HttpTestServer(200)) {
            String target = HttpCheckFixture.target(server.port());
            MvcResult created = createThroughApi(target);
            JsonNode createdBody = jsonOf(created);
            UUID id = UUID.fromString(text(createdBody, "id"));
            Check check = checks.get(space.id(), id);
            clock.pinAt(dueAt(check));
            CheckScheduler scheduler = scheduler(HttpCheckFixture.executor(address -> true));
            LeaderLease leaderLease = lease(grant());
            CheckSchedulerLoop loop = new CheckSchedulerLoop(leaderLease, scheduler, clock);

            // when
            loop.runOnce();
            consumer.consumeBatch();
            MvcResult listed = mockMvc().perform(listRequest()).andReturn();
            JsonNode page = jsonOf(listed);
            JsonNode item = page.path("items").path(0);

            // then
            assertThat(statusOf(listed)).isEqualTo(200);
            assertThat(server.requests()).isEqualTo(1);
            assertThat(text(item, "id")).isEqualTo(id.toString());
            assertThat(text(item, "state")).isEqualTo(CheckState.UP.name());
        }
    }

    /**
     * The shared database holds checks of the other tests, so the scheduler of this test sees only
     * the checks of its own space. The production scheduler has no space scope; this one exists
     * only to keep the test isolated.
     */
    private CheckStore scopedStore(UUID spaceId) {
        CheckRepository repository = bean(CheckRepository.class);
        CheckQueries queries = bean(CheckQueries.class);
        CheckResultStore resultStore = bean(CheckResultStore.class);
        Clock clockBean = bean(Clock.class);
        return new CheckStore(repository, queries, resultStore, clockBean) {
            @Override
            public List<CheckEntity> due(Instant now) {
                return super.due(now).stream()
                        .filter(entity -> entity.spaceId().equals(spaceId))
                        .toList();
            }
        };
    }

    private static Instant dueAt(Check check) {
        return check.createdAt().plusSeconds(check.intervalSeconds());
    }

    private CheckScheduler scheduler(CheckExecutor... executors) {
        var registry = CheckExecutorsFixture.of(List.of(executors));
        return new CheckScheduler(store, registry, results, transactions, tasks, clock);
    }

    private Check createCheck(String name, int intervalSeconds) {
        CheckDraft draft = new CheckDraft(
                name, CheckType.HTTP, "https://" + name + ".test", intervalSeconds, 10, 1, null, null, null, List.of());
        return checks.create(space.id(), draft, ACTOR);
    }

    private MvcResult createThroughApi(String target) throws Exception {
        String body = "{\"name\":\"api check\",\"type\":\"HTTP\",\"target\":\"" + target
                + "\",\"intervalSeconds\":60,\"timeoutSeconds\":10,\"retries\":1,\"tags\":[]}";
        MockHttpServletRequestBuilder request = post(SPACES_PATH + "/" + slug + "/checks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(csrf())
                .with(login());
        return mockMvc().perform(request).andReturn();
    }

    private MockHttpServletRequestBuilder listRequest() {
        return get(SPACES_PATH + "/" + slug + "/checks").with(login());
    }

    private long resultCount(UUID checkId) {
        return jdbc().sql("SELECT COUNT(*) FROM check_result WHERE check_id = :checkId")
                .param("checkId", checkId)
                .query(Long.class)
                .single();
    }

    private Optional<LeaseGrant> grant() {
        return Optional.of(new LeaseGrant(
                CheckSchedulerLoop.LEASE_NAME, "holder", 1L, OffsetDateTime.parse("2026-01-01T00:00:00Z")));
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

    private RequestPostProcessor login() {
        return oidcLogin().idToken(token -> token.subject(subject).issuer(ISSUER));
    }

    private static JsonNode jsonOf(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        return MAPPER.readTree(content);
    }

    private static String text(JsonNode node, String field) {
        return node.path(field).asString();
    }

    private static int statusOf(MvcResult result) {
        return result.getResponse().getStatus();
    }

    private static String uniqueSlug(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String uniqueSubject(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private static final class RecordingExecutor implements CheckExecutor {

        private final Clock clock;

        private final Consumer<Check> action;

        private final AtomicInteger calls = new AtomicInteger();

        RecordingExecutor(Clock clock) {
            this(clock, check -> {});
        }

        RecordingExecutor(Clock clock, Consumer<Check> action) {
            this.clock = clock;
            this.action = action;
        }

        @Override
        public CheckType type() {
            return CheckType.HTTP;
        }

        @Override
        public CheckResult execute(Check check) {
            calls.incrementAndGet();
            action.accept(check);
            Instant checkedAt = clock.instant();
            return new CheckResult(null, checkedAt, CheckState.UP, null, 200, 5, "HTTP/2", null);
        }

        @Override
        public CheckFormDescriptor formDescriptor() {
            return new CheckFormDescriptor(CheckType.HTTP, List.of());
        }

        int calls() {
            return calls.get();
        }
    }
}
