package dev.wakey.api.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import dev.wakey.checks.Check;
import dev.wakey.checks.CheckDraft;
import dev.wakey.checks.CheckResult;
import dev.wakey.checks.CheckResults;
import dev.wakey.checks.CheckService;
import dev.wakey.checks.CheckState;
import dev.wakey.checks.CheckType;
import dev.wakey.checks.ErrorCategory;
import dev.wakey.checks.PlanLimits;
import dev.wakey.spaces.PutResult;
import dev.wakey.spaces.Space;
import dev.wakey.spaces.SpacePrincipal;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceService;
import dev.wakey.testsupport.SecurityIntegrationTest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
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

    private static final SpacePrincipal FIXTURE_ADMIN = new SpacePrincipal("fixture-admin", true);

    private static final PlanLimits UNLIMITED = new PlanLimits(null, null, null, null, "default");

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
        assertThat(text(intervalBody, "code")).isEqualTo("validation_failed");
        assertThat(text(intervalError, "pointer")).isEqualTo("/intervalSeconds");
        assertThat(text(intervalError, "code")).isEqualTo("invalid_interval");
        assertThat(statusOf(badTimeout)).isEqualTo(400);
        assertThat(text(timeoutError, "pointer")).isEqualTo("/timeoutSeconds");
        assertThat(text(timeoutError, "code")).isEqualTo("invalid_timeout");
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
        MvcResult update = putCheck(viewer, slug, id, updateBody("renamed", "https://visible.test", 1L));
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
        assertThat(text(listBody, "code")).isEqualTo("not_found");
        assertThat(statusOf(counts)).isEqualTo(404);
        assertThat(text(countsBody, "code")).isEqualTo("not_found");
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
        assertThat(text(readBody, "code")).isEqualTo("not_found");
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

    private MvcResult putCheck(String subject, String slug, String id, String body) throws Exception {
        MockHttpServletRequestBuilder request = put(checkPath(slug, id))
                .contentType(MediaType.APPLICATION_JSON)
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
        PutResult put = spaces.put(FIXTURE_ADMIN, slug, "Checks");
        spaces.addMember(FIXTURE_ADMIN, slug, member, role);
        return put.space();
    }

    private Check seedCheck(Space space, String name, CheckType type, String target) {
        CheckDraft draft = new CheckDraft(name, type, target, 60, 10, 1, null, null, null, List.of());
        return bean(CheckService.class).create(space.id(), draft, UNLIMITED);
    }

    private Check seedCheck(Space space, String name, CheckType type, String target, String group, String tag) {
        List<String> tags = List.of(tag);
        CheckDraft draft = new CheckDraft(name, type, target, 60, 10, 1, group, null, null, tags);
        return bean(CheckService.class).create(space.id(), draft, UNLIMITED);
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
        assertThat(text(body, "code")).isEqualTo("forbidden");
    }

    private static RequestPostProcessor login(String subject) {
        return oidcLogin().idToken(token -> token.subject(subject));
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

    private static String updateBody(String name, String target, long version) {
        return "{\"name\":\"" + name + "\",\"type\":\"HTTP\",\"target\":\"" + target
                + "\",\"intervalSeconds\":60,\"timeoutSeconds\":10,\"retries\":1,\"tags\":[],\"version\":"
                + version + "}";
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
