package dev.wakey.checks.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import dev.wakey.audit.ActorKind;
import dev.wakey.audit.AuditActor;
import dev.wakey.audit.AuditLog;
import dev.wakey.audit.AuditPage;
import dev.wakey.audit.AuditRecord;
import dev.wakey.checks.Check;
import dev.wakey.checks.CheckDraft;
import dev.wakey.checks.CheckError;
import dev.wakey.checks.CheckResult;
import dev.wakey.checks.CheckResults;
import dev.wakey.checks.CheckService;
import dev.wakey.checks.CheckState;
import dev.wakey.checks.CheckType;
import dev.wakey.checks.ErrorCategory;
import dev.wakey.checks.PausedReason;
import dev.wakey.spaces.PutResult;
import dev.wakey.spaces.Space;
import dev.wakey.spaces.SpaceMember;
import dev.wakey.spaces.SpaceMembers;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceService;
import dev.wakey.testsupport.SecurityIntegrationTest;
import dev.wakey.tokens.ApiTokens;
import dev.wakey.tokens.CreatedToken;
import dev.wakey.tokens.TokenOptions;
import dev.wakey.tokens.TokenOwner;
import dev.wakey.tokens.TokenScope;
import dev.wakey.web.CommonError;
import dev.wakey.web.CurrentUser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** Keeps the HTTP checks to error mapping, access rules and response shape; the domain rules live in the module tests. */
class ChecksControllerIntegrationTest extends SecurityIntegrationTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static final String SPACES_PATH = "/api/v1/spaces";

    private static final String PROBLEM_JSON = "application/problem+json";

    private static final Instant CHECKED_AT = Instant.parse("2026-01-01T00:00:00Z");

    private static final String ISSUER = "https://issuer.example.com";

    private static final CurrentUser FIXTURE_ADMIN = new CurrentUser(ISSUER, "fixture-admin", true);

    private static final AuditActor ACTOR = AuditActor.of("checks-http-test", false);

    @Test
    void editorCreatesACheckAndReadsItBack() throws Exception {
        // given
        String editor = uniqueSubject("editor");
        String slug = uniqueSlug("create");
        Space space = seedSpace(slug, editor, SpaceRole.EDITOR);
        String body = checkBody("api health", "HTTP", "https://api.example.test/health");

        // when
        MvcResult created = postCheck(editor, slug, body);
        JsonNode createdBody = jsonOf(created);
        String id = text(createdBody, "id");
        Check check = bean(CheckService.class).get(space.id(), UUID.fromString(id));
        recordResult(check, CheckState.UP);
        MvcResult read = getCheck(editor, slug, id);
        JsonNode readBody = jsonOf(read);
        JsonNode lastResult = readBody.path("lastResult");

        // then
        assertThat(statusOf(created)).isEqualTo(201);
        assertThat(text(createdBody, "name")).isEqualTo("api health");
        assertThat(text(createdBody, "state")).isEqualTo("UNKNOWN");
        assertThat(number(createdBody, "version")).isEqualTo(1L);
        assertThat(statusOf(read)).isEqualTo(200);
        assertThat(text(readBody, "id")).isEqualTo(id);
        assertThat(text(readBody, "state")).isEqualTo("UP");
        assertThat(text(lastResult, "state")).isEqualTo("UP");
    }

    @Test
    void listIsFilteredAndPagedByCursor() throws Exception {
        // given
        String editor = uniqueSubject("editor");
        String slug = uniqueSlug("list");
        Space space = seedSpace(slug, editor, SpaceRole.EDITOR);
        Check alpha = seedCheck(space, "alpha", CheckType.HTTP, "https://alpha.test", "web", "prod");
        Check bravo = seedCheck(space, "bravo", CheckType.TCP, "db.example.test:5432", "data", "db");
        seedCheck(space, "charlie", CheckType.HTTP, "https://charlie.test", "web", "prod");
        seedCheck(space, "delta", CheckType.HTTP, "https://delta.test", "web", "prod");
        seedCheck(space, "echo", CheckType.CERTIFICATE, "echo.test", "web", "prod");
        recordResult(alpha, CheckState.UP);
        recordResult(bravo, CheckState.DOWN);

        // when
        List<String> allNames = new ArrayList<>();
        @Nullable String cursor = null;
        int pages = 0;
        do {
            String query = cursor == null ? "limit=2" : "limit=2&cursor=" + cursor;
            MvcResult result = listChecks(editor, slug, query);
            JsonNode page = jsonOf(result);
            allNames.addAll(namesOf(page));
            cursor = nextCursor(page);
            pages++;
        } while (cursor != null && pages < 100);
        MvcResult byGroup = listChecks(editor, slug, "group=web");
        MvcResult byState = listChecks(editor, slug, "state=DOWN");
        MvcResult byText = listChecks(editor, slug, "q=alpha");
        List<String> groupNames = namesOf(jsonOf(byGroup));
        List<String> stateNames = namesOf(jsonOf(byState));
        List<String> textNames = namesOf(jsonOf(byText));

        // then
        assertThat(pages).isGreaterThan(1);
        assertThat(allNames).isSorted().doesNotHaveDuplicates();
        assertThat(allNames).containsExactly("alpha", "bravo", "charlie", "delta", "echo");
        assertThat(groupNames).containsExactly("alpha", "charlie", "delta", "echo");
        assertThat(stateNames).containsExactly("bravo");
        assertThat(textNames).containsExactly("alpha");
    }

    @Test
    void aMalformedCursorOfTheCheckListIsRejected() throws Exception {
        // given
        String editor = uniqueSubject("editor");
        String slug = uniqueSlug("cursor");
        seedSpace(slug, editor, SpaceRole.EDITOR);

        // when
        MvcResult result = listChecks(editor, slug, "cursor=not-base64!");
        JsonNode body = jsonOf(result);

        // then
        assertThat(statusOf(result)).isEqualTo(400);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(text(body, "code")).isEqualTo(CommonError.INVALID_CURSOR.getCode());
    }

    @Test
    void invalidCheckIsAValidationProblemWithPointers() throws Exception {
        // given
        String editor = uniqueSubject("editor");
        String slug = uniqueSlug("invalid");
        seedSpace(slug, editor, SpaceRole.EDITOR);

        // when
        MvcResult badInterval = postCheck(editor, slug, intervalBody(0, 10));
        MvcResult badTimeout = postCheck(editor, slug, intervalBody(60, 61));
        JsonNode intervalBody = jsonOf(badInterval);
        JsonNode timeoutBody = jsonOf(badTimeout);
        JsonNode intervalError = intervalBody.path("errors").path(0);
        JsonNode timeoutError = timeoutBody.path("errors").path(0);

        // then
        assertThat(statusOf(badInterval)).isEqualTo(400);
        assertThat(contentTypeOf(badInterval)).isEqualTo(PROBLEM_JSON);
        assertThat(text(intervalBody, "code")).isEqualTo(CommonError.VALIDATION_FAILED.getCode());
        assertThat(text(intervalError, "pointer")).isEqualTo("/intervalSeconds");
        assertThat(text(intervalError, "code")).isEqualTo("min");
        assertThat(statusOf(badTimeout)).isEqualTo(400);
        assertThat(text(timeoutError, "pointer")).isEqualTo("/timeoutSeconds");
        assertThat(text(timeoutError, "code")).isEqualTo(CheckError.INVALID_TIMEOUT.getCode());
    }

    @Test
    void aCheckNameLongerThanTheLimitIsRejectedWithTheSizeCode() throws Exception {
        // given
        String editor = uniqueSubject("editor");
        String slug = uniqueSlug("bad-name");
        seedSpace(slug, editor, SpaceRole.EDITOR);
        String tooLongName = "x".repeat(101);
        String body = checkBody(tooLongName, "HTTP", "https://bad-name.test");

        // when
        MvcResult result = postCheck(editor, slug, body);
        JsonNode response = jsonOf(result);
        JsonNode error = response.path("errors").path(0);

        // then
        assertThat(statusOf(result)).isEqualTo(400);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(text(response, "code")).isEqualTo(CommonError.VALIDATION_FAILED.getCode());
        assertThat(text(error, "pointer")).isEqualTo("/name");
        assertThat(text(error, "code")).isEqualTo("size");
    }

    @Test
    void anUpdateTakesTheVersionAsAQueryParameter() throws Exception {
        // given
        String editor = uniqueSubject("editor");
        String slug = uniqueSlug("update-version");
        Space space = seedSpace(slug, editor, SpaceRole.EDITOR);
        Check check = seedCheck(space, "versioned", CheckType.HTTP, "https://versioned.test");
        String id = check.id().toString();
        String body = updateBody("versioned renamed", "https://versioned.test");

        // when
        MvcResult result = putCheck(editor, slug, id, 1L, body);
        JsonNode response = jsonOf(result);

        // then
        assertThat(statusOf(result)).isEqualTo(200);
        assertThat(text(response, "name")).isEqualTo("versioned renamed");
        assertThat(number(response, "version")).isEqualTo(2L);
    }

    @Test
    void anUpdateWithoutAVersionIsRejected() throws Exception {
        // given
        String editor = uniqueSubject("editor");
        String slug = uniqueSlug("update-no-version");
        Space space = seedSpace(slug, editor, SpaceRole.EDITOR);
        Check check = seedCheck(space, "unversioned", CheckType.HTTP, "https://unversioned.test");
        String id = check.id().toString();
        String body = updateBody("unversioned renamed", "https://unversioned.test");

        // when
        MvcResult result = putCheck(editor, slug, id, null, body);
        JsonNode response = jsonOf(result);

        // then
        assertThat(statusOf(result)).isEqualTo(400);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(text(response, "code")).isEqualTo(CommonError.BAD_REQUEST.getCode());
    }

    @Test
    void aStaleCheckVersionIsAConflictFromTheAdvice() throws Exception {
        // given
        String editor = uniqueSubject("editor");
        String slug = uniqueSlug("stale-advice");
        Space space = seedSpace(slug, editor, SpaceRole.EDITOR);
        Check check = seedCheck(space, "stale advice", CheckType.HTTP, "https://stale-advice.test");
        String id = check.id().toString();
        putCheck(editor, slug, id, 1L, updateBody("renamed once", "https://stale-advice.test"));

        // when
        MvcResult result = putCheck(editor, slug, id, 1L, updateBody("renamed twice", "https://stale-advice.test"));
        JsonNode response = jsonOf(result);

        // then
        assertThat(statusOf(result)).isEqualTo(409);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(text(response, "code")).isEqualTo(CommonError.VERSION_CONFLICT.getCode());
        assertThat(text(response, "detail")).isEqualTo(CommonError.VERSION_CONFLICT.getDetail());
    }

    @Test
    void aRunbookUrlThatIsNotHttpIsRejectedWithTheHttpUrlCode() throws Exception {
        // given
        String editor = uniqueSubject("editor");
        String slug = uniqueSlug("runbook");
        seedSpace(slug, editor, SpaceRole.EDITOR);
        String body = runbookBody("ftp://runbook.example.test/x");

        // when
        MvcResult result = postCheck(editor, slug, body);
        JsonNode response = jsonOf(result);
        JsonNode error = response.path("errors").path(0);

        // then
        assertThat(statusOf(result)).isEqualTo(400);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(text(response, "code")).isEqualTo(CommonError.VALIDATION_FAILED.getCode());
        assertThat(text(error, "pointer")).isEqualTo("/runbookUrl");
        assertThat(text(error, "code")).isEqualTo("http_url");
    }

    @Test
    void aTooLongSearchTextIsRejectedAsAValidationProblem() throws Exception {
        // given
        String editor = uniqueSubject("editor");
        String slug = uniqueSlug("long-search");
        seedSpace(slug, editor, SpaceRole.EDITOR);
        String query = "q=" + "x".repeat(101);

        // when
        MvcResult result = listChecks(editor, slug, query);
        JsonNode response = jsonOf(result);
        JsonNode error = response.path("errors").path(0);

        // then
        assertThat(statusOf(result)).isEqualTo(400);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(text(response, "code")).isEqualTo(CommonError.VALIDATION_FAILED.getCode());
        assertThat(text(error, "pointer")).isEqualTo("/q");
        assertThat(text(error, "code")).isEqualTo("size");
    }

    @Test
    void viewerReadsButCannotWriteChecks() throws Exception {
        // given
        String viewer = uniqueSubject("viewer");
        String slug = uniqueSlug("viewer");
        Space space = seedSpace(slug, viewer, SpaceRole.VIEWER);
        Check check = seedCheck(space, "visible", CheckType.HTTP, "https://visible.test");
        String id = check.id().toString();

        // when
        MvcResult list = listChecks(viewer, slug, "");
        MvcResult read = getCheck(viewer, slug, id);
        MvcResult create = postCheck(viewer, slug, checkBody("new", "HTTP", "https://new.test"));
        MvcResult update = putCheck(viewer, slug, id, 1L, updateBody("renamed", "https://visible.test"));
        MvcResult pause = postAt(viewer, checkPath(slug, id) + "/pause", pauseBody());
        MvcResult delete = deleteCheck(viewer, slug, id);

        // then
        assertThat(statusOf(list)).isEqualTo(200);
        assertThat(statusOf(read)).isEqualTo(200);
        assertForbidden(create);
        assertForbidden(update);
        assertForbidden(pause);
        assertForbidden(delete);
    }

    @Test
    void strangerGetsNotFoundForTheChecksOfAnotherSpace() throws Exception {
        // given
        String owner = uniqueSubject("owner");
        String stranger = uniqueSubject("stranger");
        String slug = uniqueSlug("stranger");
        Space space = seedSpace(slug, owner, SpaceRole.EDITOR);
        seedCheck(space, "private", CheckType.HTTP, "https://private.test");

        // when
        MvcResult list = listChecks(stranger, slug, "");
        MvcResult counts = countChecks(stranger, slug, "");
        JsonNode listBody = jsonOf(list);
        JsonNode countsBody = jsonOf(counts);

        // then
        assertThat(statusOf(list)).isEqualTo(404);
        assertThat(contentTypeOf(list)).isEqualTo(PROBLEM_JSON);
        assertThat(text(listBody, "code")).isEqualTo(CommonError.NOT_FOUND.getCode());
        assertThat(statusOf(counts)).isEqualTo(404);
        assertThat(text(countsBody, "code")).isEqualTo(CommonError.NOT_FOUND.getCode());
    }

    @Test
    void pausingAndResumingChangesTheState() throws Exception {
        // given
        String editor = uniqueSubject("editor");
        String slug = uniqueSlug("pause");
        Space space = seedSpace(slug, editor, SpaceRole.EDITOR);
        Check check = seedCheck(space, "pausable", CheckType.HTTP, "https://pausable.test");
        String id = check.id().toString();

        // when
        MvcResult paused = postAt(editor, checkPath(slug, id) + "/pause", pauseBody());
        MvcResult resumed = postAt(editor, checkPath(slug, id) + "/resume", "");
        MvcResult read = getCheck(editor, slug, id);
        JsonNode pausedBody = jsonOf(paused);
        JsonNode resumedBody = jsonOf(resumed);
        JsonNode readBody = jsonOf(read);
        JsonNode pausedReason = resumedBody.path("pausedReason");

        // then
        assertThat(statusOf(paused)).isEqualTo(200);
        assertThat(flag(pausedBody, "paused")).isTrue();
        assertThat(text(pausedBody, "pausedReason")).isEqualTo("MANUAL");
        assertThat(statusOf(resumed)).isEqualTo(200);
        assertThat(flag(resumedBody, "paused")).isFalse();
        assertThat(pausedReason.isNull()).isTrue();
        assertThat(flag(readBody, "paused")).isFalse();
    }

    @Test
    void aCheckChangedWithATokenIsAuditedAsATokenAction() throws Exception {
        // given
        String subject = uniqueSubject("token-audit");
        String slug = uniqueSlug("token-audit");
        Space space = seedSpace(slug, subject, SpaceRole.EDITOR);
        Check check = seedCheck(space, "token audited", CheckType.HTTP, "https://token-audited.test");
        CreatedToken token = bean(ApiTokens.class)
                .create(new TokenOwner(ISSUER, subject), slug, "audit", new TokenOptions(TokenScope.WRITE, null));

        // when
        String id = check.id().toString();
        MockHttpServletRequestBuilder request = post(checkPath(slug, id) + "/pause")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pauseBody())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.secret());
        MvcResult result = mockMvc().perform(request).andReturn();
        List<AuditRecord> history = bean(AuditLog.class).history(space.id(), "check", id, new AuditPage(null, 20));

        // then
        assertThat(statusOf(result)).isEqualTo(200);
        AuditRecord pause = history.stream()
                .filter(record -> record.action().equals("check.pause"))
                .findFirst()
                .orElseThrow();
        AuditActor actor = pause.actor();
        assertThat(actor.kind()).isEqualTo(ActorKind.TOKEN);
        assertThat(actor.subject()).isEqualTo(subject);
    }

    @Test
    void deletingACheckRemovesIt() throws Exception {
        // given
        String editor = uniqueSubject("editor");
        String slug = uniqueSlug("delete");
        Space space = seedSpace(slug, editor, SpaceRole.EDITOR);
        Check check = seedCheck(space, "deleted", CheckType.HTTP, "https://deleted.test");
        String id = check.id().toString();

        // when
        MvcResult deleted = deleteCheck(editor, slug, id);
        MvcResult read = getCheck(editor, slug, id);
        JsonNode readBody = jsonOf(read);

        // then
        assertThat(statusOf(deleted)).isEqualTo(204);
        assertThat(statusOf(read)).isEqualTo(404);
        assertThat(text(readBody, "code")).isEqualTo(CommonError.NOT_FOUND.getCode());
    }

    @Test
    void aBulkPauseAppliesToEveryExistingCheck() throws Exception {
        // given
        String editor = uniqueSubject("editor");
        String slug = uniqueSlug("bulk-pause");
        Space space = seedSpace(slug, editor, SpaceRole.EDITOR);
        Check first = seedCheck(space, "first", CheckType.HTTP, "https://first.test");
        Check second = seedCheck(space, "second", CheckType.HTTP, "https://second.test");
        List<UUID> ids = List.of(first.id(), second.id(), first.id());
        String body = bulkBody("PAUSE", ids);

        // when
        MvcResult result = bulk(editor, slug, body);
        JsonNode response = jsonOf(result);
        Check firstAfter = bean(CheckService.class).get(space.id(), first.id());
        Check secondAfter = bean(CheckService.class).get(space.id(), second.id());

        // then
        assertThat(statusOf(result)).isEqualTo(200);
        assertThat(number(response, "affected")).isEqualTo(2L);
        assertThat(firstAfter.paused()).isTrue();
        assertThat(firstAfter.pausedReason()).isEqualTo(PausedReason.MANUAL);
        assertThat(secondAfter.paused()).isTrue();
    }

    @Test
    void aBulkActionAnswersTheAffectedCount() throws Exception {
        // given
        String editor = uniqueSubject("editor");
        String slug = uniqueSlug("bulk-affected");
        Space space = seedSpace(slug, editor, SpaceRole.EDITOR);
        Check first = seedCheck(space, "first", CheckType.HTTP, "https://first.test");
        Check second = seedCheck(space, "second", CheckType.HTTP, "https://second.test");
        List<UUID> ids = List.of(first.id(), second.id());
        String body = bulkBody("PAUSE", ids);

        // when
        MvcResult result = bulk(editor, slug, body);
        JsonNode response = jsonOf(result);

        // then
        assertThat(statusOf(result)).isEqualTo(200);
        assertThat(contentTypeOf(result)).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(number(response, "affected")).isEqualTo(2L);
        boolean missing = response.path("missing").isMissingNode();
        assertThat(missing).isTrue();
    }

    @Test
    void aBulkActionByFilterChangesEveryMatchingCheck() throws Exception {
        // given
        String editor = uniqueSubject("editor");
        String slug = uniqueSlug("bulk-filter");
        Space space = seedSpace(slug, editor, SpaceRole.EDITOR);
        Check first = seedCheck(space, "first", CheckType.HTTP, "https://first.test", "web", "prod");
        Check second = seedCheck(space, "second", CheckType.HTTP, "https://second.test", "web", "prod");
        Check other = seedCheck(space, "other", CheckType.HTTP, "https://other.test", "data", "db");
        String body = bulkFilterBody("PAUSE", "{\"group\":\"web\"}", 2);

        // when
        MvcResult result = bulk(editor, slug, body);
        JsonNode response = jsonOf(result);
        Check firstAfter = bean(CheckService.class).get(space.id(), first.id());
        Check secondAfter = bean(CheckService.class).get(space.id(), second.id());
        Check otherAfter = bean(CheckService.class).get(space.id(), other.id());

        // then
        assertThat(statusOf(result)).isEqualTo(200);
        assertThat(number(response, "affected")).isEqualTo(2L);
        assertThat(firstAfter.paused()).isTrue();
        assertThat(secondAfter.paused()).isTrue();
        assertThat(otherAfter.paused()).isFalse();
    }

    @Test
    void aBulkActionByFilterNeedsTheExpectedCount() throws Exception {
        // given
        String editor = uniqueSubject("editor");
        String slug = uniqueSlug("bulk-expected");
        Space space = seedSpace(slug, editor, SpaceRole.EDITOR);
        seedCheck(space, "first", CheckType.HTTP, "https://first.test", "web", "prod");
        String body = "{\"action\":\"PAUSE\",\"filter\":{\"group\":\"web\"}}";

        // when
        MvcResult result = bulk(editor, slug, body);
        JsonNode response = jsonOf(result);
        JsonNode error = response.path("errors").path(0);

        // then
        assertThat(statusOf(result)).isEqualTo(400);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(text(response, "code")).isEqualTo(CommonError.VALIDATION_FAILED.getCode());
        assertThat(text(error, "pointer")).isEqualTo("/expectedCount");
    }

    @Test
    void aBulkActionByFilterChangesNothingWhenTheCountChanged() throws Exception {
        // given
        String editor = uniqueSubject("editor");
        String slug = uniqueSlug("bulk-count-changed");
        Space space = seedSpace(slug, editor, SpaceRole.EDITOR);
        Check first = seedCheck(space, "first", CheckType.HTTP, "https://first.test", "web", "prod");
        Check second = seedCheck(space, "second", CheckType.HTTP, "https://second.test", "web", "prod");
        String body = bulkFilterBody("PAUSE", "{\"group\":\"web\"}", 3);

        // when
        MvcResult result = bulk(editor, slug, body);
        JsonNode response = jsonOf(result);
        JsonNode params = response.path("params");
        Check firstAfter = bean(CheckService.class).get(space.id(), first.id());
        Check secondAfter = bean(CheckService.class).get(space.id(), second.id());
        String spaceId = space.id().toString();
        List<AuditRecord> events = bean(AuditLog.class).history(space.id(), "space", spaceId, new AuditPage(null, 50));

        // then
        assertThat(statusOf(result)).isEqualTo(409);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(text(response, "code")).isEqualTo(CheckError.BULK_COUNT_CHANGED.getCode());
        assertThat(number(params, "expected")).isEqualTo(3L);
        assertThat(number(params, "actual")).isEqualTo(2L);
        assertThat(firstAfter.paused()).isFalse();
        assertThat(secondAfter.paused()).isFalse();
        assertThat(events).noneMatch(record -> record.action().equals("check.bulk"));
    }

    @Test
    void aBulkActionNeedsExactlyOneOfIdsAndFilter() throws Exception {
        // given
        String editor = uniqueSubject("editor");
        String slug = uniqueSlug("bulk-selector");
        Space space = seedSpace(slug, editor, SpaceRole.EDITOR);
        Check check = seedCheck(space, "selected", CheckType.HTTP, "https://selected.test");
        List<UUID> ids = List.of(check.id());
        String both = bulkFilterBody("PAUSE", "{\"group\":\"web\"}", ids);
        String neither = "{\"action\":\"PAUSE\"}";

        // when
        MvcResult bothResult = bulk(editor, slug, both);
        MvcResult neitherResult = bulk(editor, slug, neither);
        JsonNode bothBody = jsonOf(bothResult);
        JsonNode neitherBody = jsonOf(neitherResult);

        // then
        assertThat(statusOf(bothResult)).isEqualTo(400);
        assertThat(text(bothBody, "code")).isEqualTo(CommonError.VALIDATION_FAILED.getCode());
        JsonNode bothError = bothBody.path("errors").path(0);
        assertThat(text(bothError, "pointer")).isEqualTo("/filter");
        assertThat(statusOf(neitherResult)).isEqualTo(400);
        assertThat(text(neitherBody, "code")).isEqualTo(CommonError.VALIDATION_FAILED.getCode());
        JsonNode neitherError = neitherBody.path("errors").path(0);
        assertThat(text(neitherError, "pointer")).isEqualTo("/ids");
    }

    @Test
    void aBulkActionWritesOneAuditEvent() throws Exception {
        // given
        String editor = uniqueSubject("editor");
        String slug = uniqueSlug("bulk-audit");
        Space space = seedSpace(slug, editor, SpaceRole.EDITOR);
        Check first = seedCheck(space, "first", CheckType.HTTP, "https://first.test");
        Check second = seedCheck(space, "second", CheckType.HTTP, "https://second.test");
        AuditLog auditLog = bean(AuditLog.class);
        List<UUID> ids = List.of(first.id(), second.id());
        String body = bulkBody("PAUSE", ids);

        // when
        MvcResult result = bulk(editor, slug, body);
        JsonNode response = jsonOf(result);
        String spaceId = space.id().toString();
        List<AuditRecord> events = auditLog.history(space.id(), "space", spaceId, new AuditPage(null, 50));
        long bulkEvents = events.stream()
                .filter(record -> record.action().equals("check.bulk"))
                .count();
        long firstPauses = pauseEntries(auditLog, space.id(), first.id());
        long secondPauses = pauseEntries(auditLog, space.id(), second.id());

        // then
        assertThat(statusOf(result)).isEqualTo(200);
        assertThat(number(response, "affected")).isEqualTo(2L);
        assertThat(bulkEvents).isEqualTo(1L);
        assertThat(events)
                .filteredOn(record -> record.action().equals("check.bulk"))
                .allSatisfy(record -> {
                    assertThat(record.details()).contains("PAUSE", "\"affected\":2");
                    assertThat(record.objectType()).isEqualTo("space");
                    assertThat(record.objectId()).isEqualTo(spaceId);
                });
        assertThat(firstPauses).isZero();
        assertThat(secondPauses).isZero();
    }

    @Test
    void aBulkRequestWithAnIdOfAnotherSpaceChangesOnlyOwnChecks() throws Exception {
        // given
        String editor = uniqueSubject("editor");
        String slug = uniqueSlug("bulk-own");
        String foreignSlug = uniqueSlug("bulk-foreign");
        Space space = seedSpace(slug, editor, SpaceRole.EDITOR);
        Space foreign = seedSpace(foreignSlug, uniqueSubject("foreign-owner"), SpaceRole.EDITOR);
        Check own = seedCheck(space, "own", CheckType.HTTP, "https://own.test");
        Check other = seedCheck(foreign, "other", CheckType.HTTP, "https://other.test");
        List<UUID> ids = List.of(own.id(), other.id());
        String body = bulkBody("PAUSE", ids);

        // when
        MvcResult result = bulk(editor, slug, body);
        JsonNode response = jsonOf(result);
        Check ownAfter = bean(CheckService.class).get(space.id(), own.id());
        Check otherAfter = bean(CheckService.class).get(foreign.id(), other.id());

        // then
        assertThat(statusOf(result)).isEqualTo(200);
        assertThat(number(response, "affected")).isEqualTo(1L);
        assertThat(ownAfter.paused()).isTrue();
        assertThat(otherAfter.paused()).isFalse();
    }

    @Test
    void aViewerCannotRunABulkAction() throws Exception {
        // given
        String viewer = uniqueSubject("viewer");
        String slug = uniqueSlug("bulk-viewer");
        Space space = seedSpace(slug, viewer, SpaceRole.VIEWER);
        Check check = seedCheck(space, "visible", CheckType.HTTP, "https://visible.test");
        List<UUID> ids = List.of(check.id());
        String body = bulkBody("PAUSE", ids);

        // when
        MvcResult result = bulk(viewer, slug, body);
        JsonNode response = jsonOf(result);

        // then
        assertThat(statusOf(result)).isEqualTo(403);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(text(response, "code")).isEqualTo(CommonError.FORBIDDEN.getCode());
    }

    @Test
    void aBulkRequestOfMoreThanTheLimitIsRejected() throws Exception {
        // given
        String editor = uniqueSubject("editor");
        String slug = uniqueSlug("bulk-limit");
        seedSpace(slug, editor, SpaceRole.EDITOR);
        List<UUID> ids = new ArrayList<>();
        for (int index = 0; index < 101; index++) {
            ids.add(UUID.randomUUID());
        }
        String body = bulkBody("PAUSE", ids);

        // when
        MvcResult result = bulk(editor, slug, body);
        JsonNode response = jsonOf(result);
        JsonNode error = response.path("errors").path(0);

        // then
        assertThat(statusOf(result)).isEqualTo(400);
        assertThat(text(response, "code")).isEqualTo(CommonError.VALIDATION_FAILED.getCode());
        assertThat(text(error, "pointer")).isEqualTo("/ids");
    }

    @Test
    void aBulkRequestWithoutIdsIsRejected() throws Exception {
        // given
        String editor = uniqueSubject("editor");
        String slug = uniqueSlug("bulk-empty");
        seedSpace(slug, editor, SpaceRole.EDITOR);
        String body = bulkBody("PAUSE", List.of());

        // when
        MvcResult result = bulk(editor, slug, body);
        JsonNode response = jsonOf(result);
        JsonNode error = response.path("errors").path(0);

        // then
        assertThat(statusOf(result)).isEqualTo(400);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(text(response, "code")).isEqualTo(CommonError.VALIDATION_FAILED.getCode());
        assertThat(text(error, "pointer")).isEqualTo("/ids");
    }

    private MvcResult bulk(String subject, String slug, String body) throws Exception {
        return postAt(subject, checksPath(slug) + "/bulk", body);
    }

    private static String bulkBody(String action, List<UUID> ids) {
        StringBuilder json = new StringBuilder("{\"action\":\"").append(action).append("\",\"ids\":[");
        for (int index = 0; index < ids.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append('"').append(ids.get(index)).append('"');
        }
        return json.append("]}").toString();
    }

    private static String bulkFilterBody(String action, String filter, int expectedCount) {
        return "{\"action\":\"" + action + "\",\"filter\":" + filter + ",\"expectedCount\":" + expectedCount + "}";
    }

    private static String bulkFilterBody(String action, String filter, List<UUID> ids) {
        String idsJson = "["
                + ids.stream()
                        .map(id -> "\"" + id + "\"")
                        .reduce((left, right) -> left + "," + right)
                        .orElse("") + "]";
        return "{\"action\":\"" + action + "\",\"ids\":" + idsJson + ",\"filter\":" + filter + "}";
    }

    private static long pauseEntries(AuditLog auditLog, UUID spaceId, UUID id) {
        String objectId = id.toString();
        List<AuditRecord> history = auditLog.history(spaceId, "check", objectId, new AuditPage(null, 50));
        return history.stream()
                .filter(record -> record.action().equals("check.pause"))
                .count();
    }

    private MvcResult listChecks(String subject, String slug, String query) throws Exception {
        String path = checksPath(slug);
        String url = query.isEmpty() ? path : path + "?" + query;
        MockHttpServletRequestBuilder request = get(url).with(login(subject));
        return mockMvc().perform(request).andReturn();
    }

    private MvcResult countChecks(String subject, String slug, String query) throws Exception {
        String path = checksPath(slug) + "/counts";
        String url = query.isEmpty() ? path : path + "?" + query;
        MockHttpServletRequestBuilder request = get(url).with(login(subject));
        return mockMvc().perform(request).andReturn();
    }

    private MvcResult getCheck(String subject, String slug, String id) throws Exception {
        MockHttpServletRequestBuilder request = get(checkPath(slug, id)).with(login(subject));
        return mockMvc().perform(request).andReturn();
    }

    private MvcResult postCheck(String subject, String slug, String body) throws Exception {
        return postAt(subject, checksPath(slug), body);
    }

    private MvcResult putCheck(String subject, String slug, String id, @Nullable Long version, String body)
            throws Exception {
        String path = checkPath(slug, id);
        String url = version == null ? path : path + "?version=" + version;
        MockHttpServletRequestBuilder request = put(url).contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(csrf())
                .with(login(subject));
        return mockMvc().perform(request).andReturn();
    }

    private MvcResult deleteCheck(String subject, String slug, String id) throws Exception {
        MockHttpServletRequestBuilder request =
                delete(checkPath(slug, id)).with(csrf()).with(login(subject));
        return mockMvc().perform(request).andReturn();
    }

    private MvcResult postAt(String subject, String path, String body) throws Exception {
        MockHttpServletRequestBuilder request = post(path).with(csrf()).with(login(subject));
        if (!body.isEmpty()) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc().perform(request).andReturn();
    }

    private Space seedSpace(String slug, String member, SpaceRole role) {
        SpaceService spaces = bean(SpaceService.class);
        PutResult put = spaces.put(FIXTURE_ADMIN, slug, "Checks", null);
        bean(SpaceMembers.class).addMember(FIXTURE_ADMIN, slug, new SpaceMember(ISSUER, member, role), false);
        return put.space();
    }

    private Check seedCheck(Space space, String name, CheckType type, String target) {
        CheckDraft draft = new CheckDraft(name, type, target, 60, 10, 1, null, null, null, List.of());
        return bean(CheckService.class).create(space.id(), draft, ACTOR);
    }

    private Check seedCheck(Space space, String name, CheckType type, String target, String group, String tag) {
        List<String> tags = List.of(tag);
        CheckDraft draft = new CheckDraft(name, type, target, 60, 10, 1, group, null, null, tags);
        return bean(CheckService.class).create(space.id(), draft, ACTOR);
    }

    private void recordResult(Check check, CheckState state) {
        @Nullable ErrorCategory category = state == CheckState.DOWN ? ErrorCategory.TARGET_DOWN : null;
        CheckResult result = new CheckResult(null, CHECKED_AT, state, category, 200, 5, "HTTP/2", "{}");
        bean(CheckResults.class).record(check.id(), result);
    }

    private void assertForbidden(MvcResult result) throws Exception {
        JsonNode body = jsonOf(result);
        assertThat(statusOf(result)).isEqualTo(403);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(text(body, "code")).isEqualTo(CommonError.FORBIDDEN.getCode());
    }

    private static RequestPostProcessor login(String subject) {
        return oidcLogin().idToken(token -> token.subject(subject).issuer(ISSUER));
    }

    private static String checksPath(String slug) {
        return SPACES_PATH + "/" + slug + "/checks";
    }

    private static String checkPath(String slug, String id) {
        return checksPath(slug) + "/" + id;
    }

    private static String checkBody(String name, String type, String target) {
        return "{\"name\":\"" + name + "\",\"type\":\"" + type + "\",\"target\":\"" + target
                + "\",\"intervalSeconds\":60,\"timeoutSeconds\":10,\"retries\":1,\"tags\":[]}";
    }

    private static String intervalBody(int intervalSeconds, int timeoutSeconds) {
        return "{\"name\":\"interval\",\"type\":\"HTTP\",\"target\":\"https://x.test\",\"intervalSeconds\":"
                + intervalSeconds + ",\"timeoutSeconds\":" + timeoutSeconds + ",\"retries\":1,\"tags\":[]}";
    }

    private static String updateBody(String name, String target) {
        return "{\"name\":\"" + name + "\",\"type\":\"HTTP\",\"target\":\"" + target
                + "\",\"intervalSeconds\":60,\"timeoutSeconds\":10,\"retries\":1,\"tags\":[]}";
    }

    private static String runbookBody(String runbookUrl) {
        return "{\"name\":\"runbook\",\"type\":\"HTTP\",\"target\":\"https://runbook.test\",\"intervalSeconds\":60,"
                + "\"timeoutSeconds\":10,\"retries\":1,\"runbookUrl\":\"" + runbookUrl + "\",\"tags\":[]}";
    }

    private static String pauseBody() {
        return "{\"reason\":\"MANUAL\"}";
    }

    private static int statusOf(MvcResult result) {
        return result.getResponse().getStatus();
    }

    private static String contentTypeOf(MvcResult result) {
        String contentType = result.getResponse().getContentType();
        return Objects.requireNonNull(contentType);
    }

    private static JsonNode jsonOf(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        return MAPPER.readTree(content);
    }

    private static String text(JsonNode node, String field) {
        return node.path(field).asString();
    }

    private static long number(JsonNode node, String field) {
        return node.path(field).asLong();
    }

    private static boolean flag(JsonNode node, String field) {
        return node.path(field).asBoolean();
    }

    private static List<String> namesOf(JsonNode page) {
        List<String> names = new ArrayList<>();
        for (JsonNode item : page.path("items")) {
            String name = text(item, "name");
            names.add(name);
        }
        return names;
    }

    private static @Nullable String nextCursor(JsonNode body) {
        JsonNode nextCursor = body.path("nextCursor");
        if (nextCursor.isNull() || nextCursor.isMissingNode()) {
            return null;
        }
        return nextCursor.asString();
    }

    private static String uniqueSlug(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String uniqueSubject(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
