package dev.wakey.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import dev.wakey.spaces.SpacePrincipal;
import dev.wakey.spaces.SpaceService;
import dev.wakey.testsupport.PostgresIntegrationTest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CheckServiceIntegrationTest extends PostgresIntegrationTest {

    private static final SpacePrincipal ADMIN = new SpacePrincipal("checks-admin", true);

    private CheckService checks;

    private CheckResults results;

    private UUID spaceA;

    private UUID spaceB;

    @BeforeEach
    void setUp() {
        checks = bean(CheckService.class);
        results = bean(CheckResults.class);
        SpaceService spaces = bean(SpaceService.class);
        spaceA = spaces.put(ADMIN, uniqueSlug("checks-a"), "Checks A").space().id();
        spaceB = spaces.put(ADMIN, uniqueSlug("checks-b"), "Checks B").space().id();
    }

    @Test
    void createdCheckIsReadBackInItsSpace() {
        // given
        NewCheck draft = newCheck("api health", CheckType.HTTP, "https://api.example.test/health");

        // when
        Check created = checks.create(spaceA, draft);
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
        NewCheck draft = newCheck("foreign", CheckType.TCP, "db.example.test:5432");
        Check created = checks.create(spaceA, draft);
        UUID id = created.id();
        CheckChanges updateChanges = changes("foreign renamed", "db.example.test:5432");

        // when
        Throwable read = catchThrowable(() -> checks.get(spaceB, id));
        Throwable update = catchThrowable(() -> checks.update(spaceB, id, updateChanges, 1L));
        Throwable paused = catchThrowable(() -> checks.pause(spaceB, id, PausedReason.MANUAL));
        Throwable deleted = catchThrowable(() -> checks.delete(spaceB, id));
        CheckPage visible = checks.list(spaceB, CheckFilter.all(), null, 100);

        // then
        assertThat(read).isInstanceOf(CheckNotFoundException.class);
        assertThat(update).isInstanceOf(CheckNotFoundException.class);
        assertThat(paused).isInstanceOf(CheckNotFoundException.class);
        assertThat(deleted).isInstanceOf(CheckNotFoundException.class);
        assertThat(visible.items()).isEmpty();
    }

    @Test
    void updateWithAStaleVersionIsRejected() {
        // given
        NewCheck draft = newCheck("versioned", CheckType.HTTP, "https://v.example.test");
        Check created = checks.create(spaceA, draft);
        long version = created.version();
        CheckChanges firstChanges = changes("versioned renamed", "https://v.example.test");
        CheckChanges staleChanges = changes("stale", "https://v.example.test");
        UUID id = created.id();

        // when
        Check updated = checks.update(spaceA, id, firstChanges, version);
        Throwable stale = catchThrowable(() -> checks.update(spaceA, id, staleChanges, version));

        // then
        assertThat(updated.name()).isEqualTo("versioned renamed");
        assertThat(updated.version()).isEqualTo(version + 1);
        assertThat(stale).isInstanceOf(CheckVersionConflictException.class);
    }

    @Test
    void validationRejectsBadIntervalsAndTimeouts() {
        // given
        NewCheck zeroInterval =
                new NewCheck("zero interval", CheckType.HTTP, "https://x.test", 0, 10, 1, null, null, null, List.of());
        NewCheck zeroTimeout =
                new NewCheck("zero timeout", CheckType.HTTP, "https://x.test", 60, 0, 1, null, null, null, List.of());
        NewCheck overInterval = new NewCheck(
                "timeout over interval", CheckType.HTTP, "https://x.test", 60, 61, 1, null, null, null, List.of());
        NewCheck tooManyRetries = new NewCheck(
                "too many retries", CheckType.HTTP, "https://x.test", 60, 10, 11, null, null, null, List.of());
        List<NewCheck> invalid = List.of(zeroInterval, zeroTimeout, overInterval, tooManyRetries);

        // when
        List<Throwable> rejections = new ArrayList<>();
        for (NewCheck candidate : invalid) {
            rejections.add(catchThrowable(() -> checks.create(spaceA, candidate)));
        }

        // then
        assertThat(rejections).allMatch(InvalidCheckException.class::isInstance);
        InvalidCheckException zeroIntervalError = (InvalidCheckException) rejections.get(0);
        InvalidCheckException zeroTimeoutError = (InvalidCheckException) rejections.get(1);
        InvalidCheckException overIntervalError = (InvalidCheckException) rejections.get(2);
        InvalidCheckException tooManyRetriesError = (InvalidCheckException) rejections.get(3);
        assertThat(zeroIntervalError.field()).isEqualTo("intervalSeconds");
        assertThat(zeroTimeoutError.field()).isEqualTo("timeoutSeconds");
        assertThat(overIntervalError.field()).isEqualTo("timeoutSeconds");
        assertThat(tooManyRetriesError.field()).isEqualTo("retries");
    }

    @Test
    void pausingStoresTheReasonAndResumingClearsIt() {
        // given
        NewCheck draft = newCheck("paused", CheckType.HTTP, "https://p.test");
        Check created = checks.create(spaceA, draft);
        UUID id = created.id();

        // when
        checks.pause(spaceA, id, PausedReason.PLAN_LIMIT);
        Check paused = checks.get(spaceA, id);
        checks.resume(spaceA, id);
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
        NewCheck bravo = newCheck("bravo", CheckType.HTTP, "https://b.test");
        NewCheck alpha = newCheck("alpha", CheckType.HTTP, "https://a.test");
        NewCheck delta = newCheck("delta", CheckType.HTTP, "https://d.test");
        NewCheck charlie = newCheck("charlie", CheckType.HTTP, "https://c.test");
        checks.create(spaceA, bravo);
        checks.create(spaceA, alpha);
        checks.create(spaceA, delta);
        checks.create(spaceA, charlie);

        // when
        CheckPage first = checks.list(spaceA, CheckFilter.all(), null, 2);
        @Nullable CheckCursor cursor = first.nextCursor();
        CheckPage second = checks.list(spaceA, CheckFilter.all(), cursor, 2);
        List<String> firstNames = names(first.items());
        List<String> secondNames = names(second.items());

        // then
        assertThat(firstNames).containsExactly("alpha", "bravo");
        assertThat(cursor).isNotNull();
        assertThat(secondNames).containsExactly("charlie", "delta");
        assertThat(second.nextCursor()).isNull();
    }

    @Test
    void listFiltersByStateTypeTagGroupAndText() {
        // given
        NewCheck alpha = newCheck("Alpha service", CheckType.HTTP, "https://alpha.test", "web", "prod");
        NewCheck bravo = newCheck("Bravo database", CheckType.TCP, "db.example.test:5432", "data", "db");
        NewCheck charlie = newCheck("Charlie certificate", CheckType.CERTIFICATE, "cert.example.test");
        Check alphaCheck = checks.create(spaceA, alpha);
        Check bravoCheck = checks.create(spaceA, bravo);
        checks.create(spaceA, charlie);
        CheckResult alphaResult = result(CheckState.UP, null, instant("2026-01-01T00:00:00Z"));
        CheckResult bravoResult = result(CheckState.DOWN, ErrorCategory.TARGET_DOWN, instant("2026-01-01T00:00:01Z"));
        results.record(alphaCheck.id(), alphaResult);
        results.record(bravoCheck.id(), bravoResult);
        checks.pause(spaceA, alphaCheck.id(), PausedReason.MANUAL);

        // when
        CheckPage byState = checks.list(spaceA, stateFilter(CheckState.UP), null, 100);
        CheckPage byType = checks.list(spaceA, typeFilter(CheckType.TCP), null, 100);
        CheckPage byTag = checks.list(spaceA, tagFilter("prod"), null, 100);
        CheckPage byGroup = checks.list(spaceA, groupFilter("data"), null, 100);
        CheckPage byText = checks.list(spaceA, textFilter("charlie"), null, 100);
        CheckPage byPaused = checks.list(spaceA, pausedFilter(PausedReason.MANUAL), null, 100);
        List<String> stateNames = names(byState.items());
        List<String> typeNames = names(byType.items());
        List<String> tagNames = names(byTag.items());
        List<String> groupNames = names(byGroup.items());
        List<String> textNames = names(byText.items());
        List<String> pausedNames = names(byPaused.items());

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
        NewCheck percentLiteral = newCheck("percent 50%", CheckType.HTTP, "https://p.test");
        NewCheck percentOther = newCheck("percent 50x", CheckType.HTTP, "https://q.test");
        NewCheck underscoreLiteral = newCheck("under_score", CheckType.HTTP, "https://r.test");
        NewCheck underscoreOther = newCheck("underXscore", CheckType.HTTP, "https://s.test");
        checks.create(spaceA, percentLiteral);
        checks.create(spaceA, percentOther);
        checks.create(spaceA, underscoreLiteral);
        checks.create(spaceA, underscoreOther);

        // when
        CheckPage percentPage = checks.list(spaceA, textFilter("%"), null, 100);
        CheckPage underscorePage = checks.list(spaceA, textFilter("_"), null, 100);
        List<String> percentNames = names(percentPage.items());
        List<String> underscoreNames = names(underscorePage.items());

        // then
        assertThat(percentNames).containsExactly("percent 50%");
        assertThat(underscoreNames).containsExactly("under_score");
    }

    @Test
    void countsFollowTheSameFiltersAsTheList() {
        // given
        NewCheck upDraft = newCheck("Alpha web", CheckType.HTTP, "https://a.test", "web", "prod");
        NewCheck downDraft = newCheck("Bravo web", CheckType.HTTP, "https://b.test", "web", "prod");
        NewCheck unknownDraft = newCheck("Charlie web", CheckType.HTTP, "https://c.test", "web", "prod");
        NewCheck otherDraft = newCheck("Delta data", CheckType.TCP, "db.example.test:5432", "data", "db");
        Check up = checks.create(spaceA, upDraft);
        Check down = checks.create(spaceA, downDraft);
        checks.create(spaceA, unknownDraft);
        checks.create(spaceA, otherDraft);
        CheckResult upResult = result(CheckState.UP, null, instant("2026-01-01T00:00:00Z"));
        CheckResult downResult = result(CheckState.DOWN, ErrorCategory.TARGET_DOWN, instant("2026-01-01T00:00:01Z"));
        results.record(up.id(), upResult);
        results.record(down.id(), downResult);

        // when
        StateCounts counts = checks.counts(spaceA, groupFilter("web"));
        CheckPage web = checks.list(spaceA, groupFilter("web"), null, 100);

        // then
        assertThat(counts.up()).isEqualTo(1L);
        assertThat(counts.degraded()).isZero();
        assertThat(counts.down()).isEqualTo(1L);
        assertThat(counts.unknown()).isEqualTo(1L);
        assertThat(web.items()).hasSize(3);
    }

    @Test
    void latestResultDecidesTheState() {
        // given
        NewCheck draft = newCheck("latest", CheckType.HTTP, "https://l.test");
        Check check = checks.create(spaceA, draft);
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
    void aCheckWithoutResultsIsUnknown() {
        // given
        NewCheck draft = newCheck("unknown", CheckType.HTTP, "https://u.test");
        Check check = checks.create(spaceA, draft);

        // when
        Check read = checks.get(spaceA, check.id());
        StateCounts counts = checks.counts(spaceA, CheckFilter.all());
        CheckPage unknownPage = checks.list(spaceA, stateFilter(CheckState.UNKNOWN), null, 100);

        // then
        assertThat(read.state()).isEqualTo(CheckState.UNKNOWN);
        assertThat(counts.unknown()).isEqualTo(1L);
        assertThat(unknownPage.items()).extracting(Check::id).containsExactly(check.id());
    }

    @Test
    void locationNetworkErrorCannotBeDown() {
        // given
        NewCheck draft = newCheck("network", CheckType.HTTP, "https://n.test");
        Check check = checks.create(spaceA, draft);
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
        assertThat(rejected).isInstanceOf(InvalidCheckException.class);
        assertThat(afterRejection).isEmpty();
        assertThat(afterTargetDown.state()).isEqualTo(CheckState.DOWN);
    }

    @Test
    void deletingACheckRemovesItsResultsAndTags() {
        // given
        List<String> tags = List.of("alpha", "beta");
        NewCheck draft = newCheckWithTags("deleted", CheckType.HTTP, "https://d.test", tags);
        Check check = checks.create(spaceA, draft);
        UUID id = check.id();
        CheckResult recorded = result(CheckState.UP, null, instant("2026-01-01T00:00:00Z"));
        results.record(id, recorded);

        // when
        checks.delete(spaceA, id);
        Throwable read = catchThrowable(() -> checks.get(spaceA, id));
        Optional<CheckResult> latest = results.latest(id);
        long tagRows = countTags(id);
        long resultRows = countResults(id);

        // then
        assertThat(read).isInstanceOf(CheckNotFoundException.class);
        assertThat(latest).isEmpty();
        assertThat(tagRows).isZero();
        assertThat(resultRows).isZero();
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

    private static List<String> names(List<Check> checks) {
        return checks.stream().map(Check::name).toList();
    }

    private static NewCheck newCheck(String name, CheckType type, String target) {
        return new NewCheck(name, type, target, 60, 10, 1, null, null, null, List.of());
    }

    private static NewCheck newCheck(String name, CheckType type, String target, String group, String tag) {
        return new NewCheck(name, type, target, 60, 10, 1, group, null, null, List.of(tag));
    }

    private static NewCheck newCheckWithTags(String name, CheckType type, String target, List<String> tags) {
        return new NewCheck(name, type, target, 60, 10, 1, null, null, null, tags);
    }

    private static CheckChanges changes(String name, String target) {
        return new CheckChanges(name, CheckType.HTTP, target, 60, 10, 1, null, null, null, List.of());
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
