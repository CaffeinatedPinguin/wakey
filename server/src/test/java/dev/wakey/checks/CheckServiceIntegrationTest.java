package dev.wakey.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import dev.wakey.audit.AuditLog;
import dev.wakey.audit.AuditOutcome;
import dev.wakey.audit.AuditPage;
import dev.wakey.audit.AuditRecord;
import dev.wakey.settings.SettingActor;
import dev.wakey.settings.SettingError;
import dev.wakey.settings.SettingValue;
import dev.wakey.settings.SpaceSettings;
import dev.wakey.spaces.SpaceService;
import dev.wakey.spaces.SpaceView;
import dev.wakey.testsupport.PostgresIntegrationTest;
import dev.wakey.web.ApiException;
import dev.wakey.web.ApiFieldError;
import dev.wakey.web.CommonError;
import dev.wakey.web.CurrentUser;
import dev.wakey.web.ErrorCode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class CheckServiceIntegrationTest extends PostgresIntegrationTest {

    private static final String ISSUER = "https://issuer.example.com";

    private static final CurrentUser ADMIN = new CurrentUser(ISSUER, "checks-admin", true);

    private static final CheckActor ACTOR = new CheckActor("checks-test");

    private CheckService checks;

    private CheckResults results;

    private AuditLog auditLog;

    private UUID spaceA;

    private UUID spaceB;

    @BeforeEach
    void setUp() {
        checks = bean(CheckService.class);
        results = bean(CheckResults.class);
        auditLog = bean(AuditLog.class);
        SpaceService spaces = bean(SpaceService.class);
        spaceA = spaces.put(ADMIN, uniqueSlug("checks-a"), "Checks A", null)
                .space()
                .id();
        spaceB = spaces.put(ADMIN, uniqueSlug("checks-b"), "Checks B", null)
                .space()
                .id();
    }

    @Test
    void createdCheckIsReadBackInItsSpace() {
        // given
        CheckDraft draft = newCheck("api health", CheckType.HTTP, "https://api.example.test/health");

        // when
        Check created = create(draft);
        Check read = checks.get(spaceA, created.id());

        // then
        assertThat(created.spaceId()).isEqualTo(spaceA);
        assertThat(created.name()).isEqualTo("api health");
        assertThat(created.type()).isEqualTo(CheckType.HTTP);
        assertThat(created.target()).isEqualTo("https://api.example.test/health");
        assertThat(created.version()).isEqualTo(1L);
        assertThat(created.state()).isEqualTo(CheckState.UNKNOWN);
        assertThat(created.paused()).isFalse();
        assertThat(read).isEqualTo(created);
    }

    @Test
    void aCheckOfAnotherSpaceIsNotFound() {
        // given
        CheckDraft draft = newCheck("foreign", CheckType.TCP, "db.example.test:5432");
        Check created = create(draft);
        UUID id = created.id();
        CheckDraft updateChanges = changes("foreign renamed", "db.example.test:5432");

        // when
        Throwable read = catchThrowable(() -> checks.get(spaceB, id));
        Throwable update = catchThrowable(() -> checks.update(spaceB, new CheckUpdate(id, updateChanges, 1L), ACTOR));
        Throwable paused = catchThrowable(() -> checks.pause(spaceB, id, PausedReason.MANUAL, ACTOR));
        Throwable deleted = catchThrowable(() -> checks.delete(spaceB, id, ACTOR));
        List<Check> visible = checks.list(spaceB, CheckFilter.all(), null, 100);

        // then
        assertApiError(read, CommonError.NOT_FOUND);
        assertApiError(update, CommonError.NOT_FOUND);
        assertApiError(paused, CommonError.NOT_FOUND);
        assertApiError(deleted, CommonError.NOT_FOUND);
        assertThat(visible).isEmpty();
    }

    @Test
    void updateWithAStaleVersionIsRejected() {
        // given
        CheckDraft draft = newCheck("versioned", CheckType.HTTP, "https://v.example.test");
        Check created = create(draft);
        long version = created.version();
        CheckDraft firstChanges = changes("versioned renamed", "https://v.example.test");
        CheckDraft staleChanges = changes("stale", "https://v.example.test");
        UUID id = created.id();

        // when
        Check updated = checks.update(spaceA, new CheckUpdate(id, firstChanges, version), ACTOR);
        Throwable stale =
                catchThrowable(() -> checks.update(spaceA, new CheckUpdate(id, staleChanges, version), ACTOR));

        // then
        assertThat(updated.name()).isEqualTo("versioned renamed");
        assertThat(updated.version()).isEqualTo(version + 1);
        assertThat(stale).isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void validationRejectsATimeoutLongerThanTheInterval() {
        // given
        CheckDraft overInterval = new CheckDraft(
                "timeout over interval", CheckType.HTTP, "https://x.test", 60, 61, 1, null, null, null, List.of());

        // when
        Throwable rejection = catchThrowable(() -> create(overInterval));

        // then
        assertInvalidField(rejection, "/timeoutSeconds", CheckError.INVALID_TIMEOUT);
    }

    @Test
    void aCheckUsesThePlanOfItsSpaceWithoutTheCallerPassingIt() {
        // given
        SpaceService spaces = bean(SpaceService.class);
        SpaceView limited = spaceWithPlan(spaces, "checks-limited", "limited");
        SpaceView open = spaceWithPlan(spaces, "checks-open", "open");
        CheckDraft draft = new CheckDraft(
                "same check", CheckType.HTTP, "https://same.test", 10, 5, 1, null, null, null, List.of());

        // when
        Throwable rejected = catchThrowable(() -> checks.create(limited.id(), draft, ACTOR));
        Check accepted = checks.create(open.id(), draft, ACTOR);
        spaces.assignPlan(ADMIN, open.slug(), "limited");
        CheckUpdate narrower = new CheckUpdate(accepted.id(), draft, accepted.version());
        Throwable updateRejected = catchThrowable(() -> checks.update(open.id(), narrower, ACTOR));

        // then
        assertApiError(rejected, SettingError.OUT_OF_PLAN);
        assertThat(accepted.spaceId()).isEqualTo(open.id());
        assertApiError(updateRejected, SettingError.OUT_OF_PLAN);
    }

    @Test
    void thePlanBoundRuleHasOneImplementation() {
        // given a space plan and one value outside its interval bound on both entry points
        SpaceService spaces = bean(SpaceService.class);
        SpaceView limited = spaceWithPlan(spaces, "checks-one-rule", "limited");
        SpaceSettings settings = bean(SpaceSettings.class);
        CheckDraft draft = new CheckDraft(
                "one rule", CheckType.HTTP, "https://one-rule.test", 10, 5, 1, null, null, null, List.of());
        SettingActor actor = new SettingActor("one-rule-test", false);
        SettingValue value = new SettingValue("check.interval", "10");

        // when
        Throwable fromCheck = catchThrowable(() -> checks.create(limited.id(), draft, ACTOR));
        Throwable fromSetting = catchThrowable(() -> settings.set(limited.id(), value, actor));

        // then
        ApiException checkError = (ApiException) fromCheck;
        ApiException settingError = (ApiException) fromSetting;
        String checkCode = checkError.getErrorCode().getCode();
        assertThat(checkCode).isEqualTo("out_of_plan");
        assertThat(checkError.getErrorCode()).isEqualTo(settingError.getErrorCode());
        assertThat(checkError.getParams()).isEqualTo(settingError.getParams());
    }

    @Test
    void aSpaceWithoutAnAssignedPlanUsesTheInstanceDefaultPlan() {
        // given
        CheckDraft draft = new CheckDraft(
                "default plan", CheckType.HTTP, "https://default-plan.test", 10, 5, 1, null, null, null, List.of());

        // when
        Check created = checks.create(spaceA, draft, ACTOR);
        Check read = checks.get(spaceA, created.id());

        // then
        assertThat(created.intervalSeconds()).isEqualTo(10);
        assertThat(created.timeoutSeconds()).isEqualTo(5);
        assertThat(created.retries()).isEqualTo(1);
        assertThat(read).isEqualTo(created);
    }

    @Test
    void pausingStoresTheReasonAndResumingClearsIt() {
        // given
        CheckDraft draft = newCheck("paused", CheckType.HTTP, "https://p.test");
        Check created = create(draft);
        UUID id = created.id();

        // when
        checks.pause(spaceA, id, PausedReason.PLAN_LIMIT, ACTOR);
        Check paused = checks.get(spaceA, id);
        checks.resume(spaceA, id, ACTOR);
        Check resumed = checks.get(spaceA, id);

        // then
        assertThat(paused.paused()).isTrue();
        assertThat(paused.pausedReason()).isEqualTo(PausedReason.PLAN_LIMIT);
        assertThat(resumed.paused()).isFalse();
        assertThat(resumed.pausedReason()).isNull();
    }

    @Test
    void listIsSortedByNameAndResumesAfterACursor() {
        // given
        CheckDraft bravo = newCheck("bravo", CheckType.HTTP, "https://b.test");
        CheckDraft alpha = newCheck("alpha", CheckType.HTTP, "https://a.test");
        CheckDraft delta = newCheck("delta", CheckType.HTTP, "https://d.test");
        CheckDraft charlie = newCheck("charlie", CheckType.HTTP, "https://c.test");
        create(bravo);
        create(alpha);
        create(delta);
        create(charlie);

        // when
        List<Check> first = checks.list(spaceA, CheckFilter.all(), null, 2);
        Check lastOfFirstPage = first.getLast();
        CheckPosition cursor = new CheckPosition(lastOfFirstPage.name(), lastOfFirstPage.id());
        List<Check> second = checks.list(spaceA, CheckFilter.all(), cursor, 2);
        List<String> firstNames = names(first);
        List<String> secondNames = names(second);

        // then
        assertThat(firstNames).containsExactly("alpha", "bravo");
        assertThat(first).hasSize(2);
        assertThat(secondNames).containsExactly("charlie", "delta");
    }

    @Test
    void listFiltersByStateTypeTagGroupAndText() {
        // given
        CheckDraft alpha = newCheck("Alpha service", CheckType.HTTP, "https://alpha.test", "web", "prod");
        CheckDraft bravo = newCheck("Bravo database", CheckType.TCP, "db.example.test:5432", "data", "db");
        CheckDraft charlie = newCheck("Charlie certificate", CheckType.CERTIFICATE, "cert.example.test");
        Check alphaCheck = create(alpha);
        Check bravoCheck = create(bravo);
        create(charlie);
        CheckResult alphaResult = result(CheckState.UP, null, instant("2026-01-01T00:00:00Z"));
        CheckResult bravoResult = result(CheckState.DOWN, ErrorCategory.TARGET_DOWN, instant("2026-01-01T00:00:01Z"));
        results.record(alphaCheck.id(), alphaResult);
        results.record(bravoCheck.id(), bravoResult);
        checks.pause(spaceA, alphaCheck.id(), PausedReason.MANUAL, ACTOR);

        // when
        List<Check> byState = checks.list(spaceA, stateFilter(CheckState.UP), null, 100);
        List<Check> byType = checks.list(spaceA, typeFilter(CheckType.TCP), null, 100);
        List<Check> byTag = checks.list(spaceA, tagFilter("prod"), null, 100);
        List<Check> byGroup = checks.list(spaceA, groupFilter("data"), null, 100);
        List<Check> byText = checks.list(spaceA, textFilter("charlie"), null, 100);
        List<Check> byPaused = checks.list(spaceA, pausedFilter(PausedReason.MANUAL), null, 100);
        List<String> stateNames = names(byState);
        List<String> typeNames = names(byType);
        List<String> tagNames = names(byTag);
        List<String> groupNames = names(byGroup);
        List<String> textNames = names(byText);
        List<String> pausedNames = names(byPaused);

        // then
        assertThat(stateNames).containsExactly("Alpha service");
        assertThat(typeNames).containsExactly("Bravo database");
        assertThat(tagNames).containsExactly("Alpha service");
        assertThat(groupNames).containsExactly("Bravo database");
        assertThat(textNames).containsExactly("Charlie certificate");
        assertThat(pausedNames).containsExactly("Alpha service");
    }

    @Test
    void searchTextTreatsLikeCharactersLiterally() {
        // given
        CheckDraft percentLiteral = newCheck("percent 50%", CheckType.HTTP, "https://p.test");
        CheckDraft percentOther = newCheck("percent 50x", CheckType.HTTP, "https://q.test");
        CheckDraft underscoreLiteral = newCheck("under_score", CheckType.HTTP, "https://r.test");
        CheckDraft underscoreOther = newCheck("underXscore", CheckType.HTTP, "https://s.test");
        create(percentLiteral);
        create(percentOther);
        create(underscoreLiteral);
        create(underscoreOther);

        // when
        List<Check> percentPage = checks.list(spaceA, textFilter("%"), null, 100);
        List<Check> underscorePage = checks.list(spaceA, textFilter("_"), null, 100);
        List<String> percentNames = names(percentPage);
        List<String> underscoreNames = names(underscorePage);

        // then
        assertThat(percentNames).containsExactly("percent 50%");
        assertThat(underscoreNames).containsExactly("under_score");
    }

    @Test
    void searchTextWithWildcardCharactersIsMatchedLiterally() {
        // given
        create(newCheck("percent 50%", CheckType.HTTP, "https://literal-percent.test"));
        create(newCheck("percent 50x", CheckType.HTTP, "https://literal-other.test"));
        create(newCheck("under_score", CheckType.HTTP, "https://literal-underscore.test"));
        create(newCheck("underXscore", CheckType.HTTP, "https://literal-x.test"));

        // when
        List<Check> percentPage = checks.list(spaceA, textFilter("%"), null, 100);
        List<Check> underscorePage = checks.list(spaceA, textFilter("_"), null, 100);
        List<String> percentNames = names(percentPage);
        List<String> underscoreNames = names(underscorePage);

        // then
        assertThat(percentNames).containsExactly("percent 50%");
        assertThat(underscoreNames).containsExactly("under_score");
    }

    @Test
    void theStateOfACheckComesFromItsNewestResult() {
        // given
        Check check = create(newCheck("state source", CheckType.HTTP, "https://state-source.test"));
        CheckResult result = result(CheckState.UP, null, instant("2026-01-01T00:00:10Z"));

        // when
        results.record(check.id(), result);
        Check read = checks.get(spaceA, check.id());

        // then
        assertThat(read.state()).isEqualTo(CheckState.UP);
    }

    @Test
    void anOlderResultNeverRevertsTheState() {
        // given
        Check check = create(newCheck("delayed result", CheckType.HTTP, "https://delayed.test"));
        UUID id = check.id();
        CheckResult newer = result(CheckState.UP, null, instant("2026-01-01T00:00:20Z"));
        CheckResult older = result(CheckState.DOWN, ErrorCategory.TARGET_DOWN, instant("2026-01-01T00:00:10Z"));

        // when
        results.record(id, newer);
        results.record(id, older);
        Check read = checks.get(spaceA, id);

        // then
        assertThat(read.state()).isEqualTo(CheckState.UP);
    }

    @Test
    void everyCheckWriteIsRecordedInTheAuditLog() {
        // given
        CheckDraft draft = newCheck("audited", CheckType.HTTP, "https://audited.test");
        Check created = create(draft);
        CheckDraft changes = changes("audited renamed", "https://audited.test");

        // when
        UUID id = created.id();
        long version = created.version();
        CheckUpdate update = new CheckUpdate(id, changes, version);
        checks.update(spaceA, update, ACTOR);
        checks.pause(spaceA, created.id(), PausedReason.MANUAL, ACTOR);
        checks.resume(spaceA, created.id(), ACTOR);
        checks.delete(spaceA, created.id(), ACTOR);
        String objectId = created.id().toString();
        List<AuditRecord> history = auditLog.history(spaceA, "check", objectId, new AuditPage(null, 20));

        // then
        assertThat(history).hasSize(5);
        assertThat(history)
                .extracting(AuditRecord::action)
                .containsExactlyInAnyOrder(
                        "check.create", "check.update", "check.pause", "check.resume", "check.delete");
        assertThat(history).allSatisfy(event -> {
            assertThat(event.objectType()).isEqualTo("check");
            assertThat(event.objectId()).isEqualTo(objectId);
            assertThat(event.spaceId()).isEqualTo(spaceA);
            assertThat(event.outcome()).isEqualTo(AuditOutcome.SUCCESS);
            assertThat(event.details()).isNotBlank();
        });
    }

    @Test
    void aRolledBackWriteLeavesNoAuditEvent() {
        // given
        CheckDraft draft = newCheck("rolled back", CheckType.HTTP, "https://rolled-back.test");
        AtomicReference<UUID> createdId = new AtomicReference<>(UUID.randomUUID());
        PlatformTransactionManager manager = bean(PlatformTransactionManager.class);
        TransactionTemplate template = new TransactionTemplate(manager);

        // when
        template.executeWithoutResult(status -> {
            Check created = create(draft);
            createdId.set(created.id());
            status.setRollbackOnly();
        });
        String objectId = createdId.get().toString();
        List<AuditRecord> history = auditLog.history(spaceA, "check", objectId, new AuditPage(null, 10));
        Throwable read = catchThrowable(() -> checks.get(spaceA, createdId.get()));

        // then
        assertThat(history).isEmpty();
        assertApiError(read, CommonError.NOT_FOUND);
    }

    @Test
    void countsFollowTheSameFiltersAsTheList() {
        // given
        CheckDraft upDraft = newCheck("Alpha web", CheckType.HTTP, "https://a.test", "web", "prod");
        CheckDraft downDraft = newCheck("Bravo web", CheckType.HTTP, "https://b.test", "web", "prod");
        CheckDraft unknownDraft = newCheck("Charlie web", CheckType.HTTP, "https://c.test", "web", "prod");
        CheckDraft otherDraft = newCheck("Delta data", CheckType.TCP, "db.example.test:5432", "data", "db");
        Check up = create(upDraft);
        Check down = create(downDraft);
        create(unknownDraft);
        create(otherDraft);
        CheckResult upResult = result(CheckState.UP, null, instant("2026-01-01T00:00:00Z"));
        CheckResult downResult = result(CheckState.DOWN, ErrorCategory.TARGET_DOWN, instant("2026-01-01T00:00:01Z"));
        results.record(up.id(), upResult);
        results.record(down.id(), downResult);

        // when
        StateCounts counts = checks.counts(spaceA, groupFilter("web"));
        List<Check> web = checks.list(spaceA, groupFilter("web"), null, 100);

        // then
        assertThat(counts.up()).isEqualTo(1L);
        assertThat(counts.degraded()).isZero();
        assertThat(counts.down()).isEqualTo(1L);
        assertThat(counts.unknown()).isEqualTo(1L);
        assertThat(web).hasSize(3);
    }

    @Test
    void latestResultDecidesTheState() {
        // given
        CheckDraft draft = newCheck("latest", CheckType.HTTP, "https://l.test");
        Check check = create(draft);
        UUID id = check.id();
        CheckResult first = result(CheckState.DOWN, ErrorCategory.TARGET_DOWN, instant("2026-01-01T00:00:00Z"));
        CheckResult second = result(CheckState.UP, null, instant("2026-01-01T00:00:10Z"));
        CheckResult third = result(CheckState.DOWN, ErrorCategory.TARGET_DOWN, instant("2026-01-01T00:00:20Z"));

        // when
        results.record(id, first);
        results.record(id, second);
        Check upToDate = checks.get(spaceA, id);
        results.record(id, third);
        Check afterNewest = checks.get(spaceA, id);
        Optional<CheckResult> latest = results.latest(id);

        // then
        assertThat(upToDate.state()).isEqualTo(CheckState.UP);
        assertThat(afterNewest.state()).isEqualTo(CheckState.DOWN);
        CheckResult latestResult = latest.orElseThrow();
        assertThat(latestResult.state()).isEqualTo(CheckState.DOWN);
    }

    @Test
    void listAndDetailsAgreeOnTheLatestResultOnATie() {
        // given
        CheckDraft draft = newCheck("tie", CheckType.HTTP, "https://tie.test");
        Check check = create(draft);
        UUID checkId = check.id();
        Instant checkedAt = instant("2026-01-01T00:00:00Z");
        UUID lowerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID higherId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        insertResult(lowerId, checkId, checkedAt, CheckState.DOWN);
        insertResult(higherId, checkId, checkedAt, CheckState.UP);

        // when
        Check listed = checks.list(spaceA, CheckFilter.all(), null, 100).getFirst();
        Check details = checks.get(spaceA, checkId);
        CheckResult latest = results.latest(checkId).orElseThrow();

        // then
        assertThat(listed.state()).isEqualTo(CheckState.UP);
        assertThat(details.state()).isEqualTo(CheckState.UP);
        assertThat(latest.state()).isEqualTo(CheckState.UP);
    }

    @Test
    void pausingAPausedCheckKeepsItsVersion() {
        // given
        CheckDraft draft = newCheck("paused twice", CheckType.HTTP, "https://pt.test");
        Check created = create(draft);
        UUID id = created.id();
        checks.pause(spaceA, id, PausedReason.MANUAL, ACTOR);
        Check firstPause = checks.get(spaceA, id);

        // when
        checks.pause(spaceA, id, PausedReason.MANUAL, ACTOR);
        Check secondPause = checks.get(spaceA, id);

        // then
        assertThat(secondPause.version()).isEqualTo(firstPause.version());
        assertThat(secondPause.paused()).isTrue();
        assertThat(secondPause.pausedReason()).isEqualTo(PausedReason.MANUAL);
    }

    @Test
    void resumingARunningCheckKeepsItsVersion() {
        // given
        CheckDraft draft = newCheck("running", CheckType.HTTP, "https://running.test");
        Check created = create(draft);
        UUID id = created.id();
        long version = created.version();

        // when
        checks.resume(spaceA, id, ACTOR);
        Check resumed = checks.get(spaceA, id);
        Throwable missing = catchThrowable(() -> checks.resume(spaceA, UUID.randomUUID(), ACTOR));

        // then
        assertThat(resumed.version()).isEqualTo(version);
        assertThat(resumed.paused()).isFalse();
        assertApiError(missing, CommonError.NOT_FOUND);
    }

    @Test
    void aNoOpPauseWritesNoAuditEvent() {
        // given
        Check created = create(newCheck("paused once", CheckType.HTTP, "https://paused-once.test"));
        UUID id = created.id();
        checks.pause(spaceA, id, PausedReason.MANUAL, ACTOR);

        // when
        checks.pause(spaceA, id, PausedReason.MANUAL, ACTOR);
        List<AuditRecord> history = auditLog.history(spaceA, "check", id.toString(), new AuditPage(null, 20));

        // then
        long pauses = history.stream()
                .filter(record -> record.action().equals("check.pause"))
                .count();
        assertThat(pauses).isEqualTo(1L);
    }

    @Test
    void bulkResumeSkipsAlreadyRunningChecksWithoutAnError() {
        // given
        Check running = create(newCheck("bulk running", CheckType.HTTP, "https://bulk-running.test"));
        Check paused = create(newCheck("bulk paused", CheckType.HTTP, "https://bulk-paused.test"));
        checks.pause(spaceA, paused.id(), PausedReason.MANUAL, ACTOR);
        List<UUID> ids = List.of(running.id(), paused.id());

        // when
        int affected = checks.bulk(spaceA, new CheckBulkCommand(CheckBulkAction.RESUME, ids, null, null), ACTOR);
        Check runningAfter = checks.get(spaceA, running.id());
        Check pausedAfter = checks.get(spaceA, paused.id());

        // then the single resume of a running check is a no-op, so bulk does not count it as applied.
        assertThat(affected).isEqualTo(1);
        assertThat(runningAfter.paused()).isFalse();
        assertThat(pausedAfter.paused()).isFalse();
    }

    @Test
    void aCheckWithoutResultsIsUnknown() {
        // given
        CheckDraft draft = newCheck("unknown", CheckType.HTTP, "https://u.test");
        Check check = create(draft);

        // when
        Check read = checks.get(spaceA, check.id());
        StateCounts counts = checks.counts(spaceA, CheckFilter.all());
        List<Check> unknownPage = checks.list(spaceA, stateFilter(CheckState.UNKNOWN), null, 100);

        // then
        assertThat(read.state()).isEqualTo(CheckState.UNKNOWN);
        assertThat(counts.unknown()).isEqualTo(1L);
        assertThat(unknownPage).extracting(Check::id).containsExactly(check.id());
    }

    @Test
    void locationNetworkErrorCannotBeDown() {
        // given
        CheckDraft draft = newCheck("network", CheckType.HTTP, "https://n.test");
        Check check = create(draft);
        UUID id = check.id();
        CheckResult rejectedResult =
                result(CheckState.DOWN, ErrorCategory.LOCATION_NETWORK, instant("2026-01-01T00:00:00Z"));
        CheckResult degradedResult =
                result(CheckState.DEGRADED, ErrorCategory.LOCATION_NETWORK, instant("2026-01-01T00:00:01Z"));
        CheckResult targetDown = result(CheckState.DOWN, ErrorCategory.TARGET_DOWN, instant("2026-01-01T00:00:02Z"));

        // when
        Throwable rejected = catchThrowable(() -> results.record(id, rejectedResult));
        Optional<CheckResult> afterRejection = results.latest(id);
        results.record(id, degradedResult);
        results.record(id, targetDown);
        Check afterTargetDown = checks.get(spaceA, id);

        // then
        assertInvalidField(rejected, "/state", CheckError.LOCATION_ERROR_CANNOT_BE_DOWN);
        assertThat(afterRejection).isEmpty();
        assertThat(afterTargetDown.state()).isEqualTo(CheckState.DOWN);
    }

    @Test
    void deletingACheckRemovesItsResultsAndTags() {
        // given
        List<String> tags = List.of("alpha", "beta");
        CheckDraft draft = newCheckWithTags("deleted", CheckType.HTTP, "https://d.test", tags);
        Check check = create(draft);
        UUID id = check.id();
        CheckResult recorded = result(CheckState.UP, null, instant("2026-01-01T00:00:00Z"));
        results.record(id, recorded);

        // when
        checks.delete(spaceA, id, ACTOR);
        Throwable read = catchThrowable(() -> checks.get(spaceA, id));
        Optional<CheckResult> latest = results.latest(id);
        long tagRows = countTags(id);
        long resultRows = countResults(id);

        // then
        assertApiError(read, CommonError.NOT_FOUND);
        assertThat(latest).isEmpty();
        assertThat(tagRows).isZero();
        assertThat(resultRows).isZero();
    }

    @Test
    void aCheckAndItsTagsAreSavedAsOneAggregate() {
        // given
        CheckDraft draft =
                newCheckWithTags("aggregate", CheckType.HTTP, "https://aggregate.test", List.of("alpha", "beta"));

        // when
        Check created = create(draft);
        Check read = checks.get(spaceA, created.id());
        long firstRows = countTags(created.id());
        CheckDraft changed =
                newCheckWithTags("aggregate renamed", CheckType.HTTP, "https://aggregate.test", List.of("gamma"));
        CheckUpdate update = new CheckUpdate(created.id(), changed, created.version());
        Check updated = checks.update(spaceA, update, ACTOR);
        long secondRows = countTags(created.id());

        // then
        assertThat(read.tags()).containsExactly("alpha", "beta");
        assertThat(firstRows).isEqualTo(2);
        assertThat(updated.tags()).containsExactly("gamma");
        assertThat(secondRows).isEqualTo(1);
    }

    @Test
    void concurrentResultsLeaveTheStateOfTheNewestOne() throws Exception {
        // given
        Check check = create(newCheck("concurrent results", CheckType.HTTP, "https://concurrent.test"));
        UUID id = check.id();
        CheckResult older = result(CheckState.DOWN, ErrorCategory.TARGET_DOWN, instant("2026-01-01T00:00:00Z"));
        CheckResult newer = result(CheckState.UP, null, instant("2026-01-01T00:00:10Z"));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        // when
        try {
            Future<Void> first = executor.submit(recordConcurrently(id, older, start));
            Future<Void> second = executor.submit(recordConcurrently(id, newer, start));
            start.countDown();
            first.get();
            second.get();
        } finally {
            executor.shutdown();
        }
        Check read = checks.get(spaceA, id);

        // then
        assertThat(read.state()).isEqualTo(CheckState.UP);
    }

    @Test
    void parallelCreatesNeverExceedThePlanCheckLimit() throws Exception {
        // given
        SpaceService spaces = bean(SpaceService.class);
        SpaceView limited = spaceWithPlan(spaces, "checks-parallel", "limited");
        CheckDraft firstDraft = newCheck("first parallel", CheckType.HTTP, "https://first-parallel.test");
        checks.create(limited.id(), firstDraft, ACTOR);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Boolean> outcomes = new ArrayList<>();

        // when
        try {
            Callable<Boolean> create = () -> {
                start.await();
                return createsOneMoreCheck(limited);
            };
            Future<Boolean> first = executor.submit(create);
            Future<Boolean> second = executor.submit(create);
            start.countDown();
            outcomes.add(first.get(120, TimeUnit.SECONDS));
            outcomes.add(second.get(120, TimeUnit.SECONDS));
        } finally {
            executor.shutdown();
        }
        long stored = checks.list(limited.id(), CheckFilter.all(), null, 100).size();

        // then
        long created = outcomes.stream().filter(Boolean::booleanValue).count();
        assertThat(created).isEqualTo(1L);
        assertThat(stored).isEqualTo(2L);
    }

    private boolean createsOneMoreCheck(SpaceView space) {
        try {
            CheckDraft draft = newCheck("parallel", CheckType.HTTP, "https://parallel.test");
            checks.create(space.id(), draft, ACTOR);
            return true;
        } catch (ApiException exception) {
            if (exception.getErrorCode() != CheckError.PLAN_LIMIT) {
                throw exception;
            }
            return false;
        }
    }

    private Callable<Void> recordConcurrently(UUID id, CheckResult result, CountDownLatch start) {
        return () -> {
            start.await();
            results.record(id, result);
            return null;
        };
    }

    private void insertResult(UUID id, UUID checkId, Instant checkedAt, CheckState state) {
        jdbc().sql("""
                        INSERT INTO check_result (id, check_id, checked_at, state)
                        VALUES (:id, :checkId, :checkedAt, :state)
                        """)
                .param("id", id)
                .param("checkId", checkId)
                .param("checkedAt", OffsetDateTime.ofInstant(checkedAt, ZoneOffset.UTC))
                .param("state", state.name())
                .update();
        jdbc().sql("""
                        UPDATE "check" SET last_state = (
                            SELECT r.state FROM check_result r WHERE r.check_id = :checkId
                            ORDER BY r.checked_at DESC, r.id DESC LIMIT 1)
                        WHERE id = :checkId
                        """).param("checkId", checkId).update();
    }

    private long countTags(UUID checkId) {
        return jdbc().sql("SELECT COUNT(*) FROM check_tag WHERE check_id = :checkId")
                .param("checkId", checkId)
                .query(Long.class)
                .single();
    }

    private long countResults(UUID checkId) {
        return jdbc().sql("SELECT COUNT(*) FROM check_result WHERE check_id = :checkId")
                .param("checkId", checkId)
                .query(Long.class)
                .single();
    }

    private Check create(CheckDraft draft) {
        return checks.create(spaceA, draft, ACTOR);
    }

    private static SpaceView spaceWithPlan(SpaceService spaces, String prefix, String planName) {
        String slug = uniqueSlug(prefix);
        spaces.put(ADMIN, slug, "Plan " + planName, null);
        spaces.assignPlan(ADMIN, slug, planName);
        return spaces.view(ADMIN, slug);
    }

    private static List<String> names(List<Check> checks) {
        return checks.stream().map(Check::name).toList();
    }

    private static void assertApiError(Throwable throwable, ErrorCode code) {
        assertThat(throwable).isInstanceOf(ApiException.class);
        assertThat(((ApiException) throwable).getErrorCode()).isEqualTo(code);
    }

    private static void assertInvalidField(Throwable throwable, String pointer, CheckError code) {
        assertThat(throwable).isInstanceOf(ApiException.class);
        ApiException exception = (ApiException) throwable;
        assertThat(exception.getErrorCode()).isEqualTo(CommonError.VALIDATION_FAILED);
        assertThat(exception.getFieldErrors()).hasSize(1);
        ApiFieldError problem = exception.getFieldErrors().getFirst();
        assertThat(problem.pointer()).isEqualTo(pointer);
        assertThat(problem.code()).isEqualTo(code.getCode());
    }

    private static CheckDraft newCheck(String name, CheckType type, String target) {
        return new CheckDraft(name, type, target, 60, 10, 1, null, null, null, List.of());
    }

    private static CheckDraft newCheck(String name, CheckType type, String target, String group, String tag) {
        return new CheckDraft(name, type, target, 60, 10, 1, group, null, null, List.of(tag));
    }

    private static CheckDraft newCheckWithTags(String name, CheckType type, String target, List<String> tags) {
        return new CheckDraft(name, type, target, 60, 10, 1, null, null, null, tags);
    }

    private static CheckDraft changes(String name, String target) {
        return new CheckDraft(name, CheckType.HTTP, target, 60, 10, 1, null, null, null, List.of());
    }

    private static CheckResult result(CheckState state, @Nullable ErrorCategory category, Instant checkedAt) {
        return new CheckResult(null, checkedAt, state, category, null, null, null, null);
    }

    private static CheckFilter stateFilter(CheckState state) {
        return new CheckFilter(null, state, null, null, null, null);
    }

    private static CheckFilter typeFilter(CheckType type) {
        return new CheckFilter(null, null, type, null, null, null);
    }

    private static CheckFilter tagFilter(String tag) {
        return new CheckFilter(null, null, null, tag, null, null);
    }

    private static CheckFilter groupFilter(String group) {
        return new CheckFilter(null, null, null, null, group, null);
    }

    private static CheckFilter textFilter(String q) {
        return new CheckFilter(q, null, null, null, null, null);
    }

    private static CheckFilter pausedFilter(PausedReason reason) {
        return new CheckFilter(null, null, null, null, null, reason);
    }

    private static Instant instant(String value) {
        return Instant.parse(value);
    }

    private static String uniqueSlug(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
