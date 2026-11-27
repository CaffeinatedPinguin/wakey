package dev.wakey.checks.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import dev.wakey.checks.CheckError;
import dev.wakey.settings.SettingError;
import dev.wakey.spaces.SpaceMember;
import dev.wakey.spaces.SpaceMembers;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceService;
import dev.wakey.testsupport.SecurityIntegrationTest;
import dev.wakey.web.CommonError;
import dev.wakey.web.CurrentUser;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class ChecksPlanLimitsIntegrationTest extends SecurityIntegrationTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static final String SPACES_PATH = "/api/v1/spaces";

    private static final String PROBLEM_JSON = "application/problem+json";

    private static final String ISSUER = "https://issuer.example.com";

    private static final CurrentUser FIXTURE_ADMIN = new CurrentUser(ISSUER, "fixture-admin", true);

    @Test
    void aCheckBelowThePlanMinimumIntervalIsRejected() throws Exception {
        // given
        String editor = uniqueSubject("plan-editor");
        String slug = uniqueSlug("plan-min");
        seedSpace(slug, editor, SpaceRole.EDITOR);
        String body = checkBody("slow", "https://slow.test", 10, 5);

        // when
        MvcResult result = postCheck(editor, slug, body);
        JsonNode response = jsonOf(result);
        JsonNode params = response.path("params");

        // then
        assertThat(statusOf(result)).isEqualTo(400);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(text(response, "code")).isEqualTo(SettingError.OUT_OF_PLAN.getCode());
        assertThat(number(params, "bound")).isEqualTo(30L);
        assertThat(text(params, "planName")).isEqualTo("limited");
        assertThat(text(params, "key")).isEqualTo("check.interval");
    }

    @Test
    void creatingMoreChecksThanThePlanAllowsIsRejected() throws Exception {
        // given
        String editor = uniqueSubject("plan-editor");
        String slug = uniqueSlug("plan-max");
        seedSpace(slug, editor, SpaceRole.EDITOR);
        String body = checkBody("healthy", "https://healthy.test", 60, 10);
        postCheck(editor, slug, body);
        postCheck(editor, slug, body);

        // when
        MvcResult result = postCheck(editor, slug, body);
        JsonNode response = jsonOf(result);
        JsonNode params = response.path("params");

        // then
        assertThat(statusOf(result)).isEqualTo(400);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(text(response, "code")).isEqualTo(CheckError.PLAN_LIMIT.getCode());
        assertThat(number(params, "bound")).isEqualTo(2L);
        assertThat(text(params, "planName")).isEqualTo("limited");
    }

    @Test
    void aNullTagIsAValidationError() throws Exception {
        // given
        String editor = uniqueSubject("plan-editor");
        String slug = uniqueSlug("plan-tags");
        seedSpace(slug, editor, SpaceRole.EDITOR);
        String body = """
                {"name":"tagged","type":"HTTP","target":"https://tagged.test","intervalSeconds":60,
                 "timeoutSeconds":10,"retries":1,"tags":[null]}
                """;

        // when
        MvcResult result = postCheck(editor, slug, body);
        JsonNode response = jsonOf(result);

        // then
        assertThat(statusOf(result)).isEqualTo(400);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(text(response, "code")).isEqualTo(CommonError.VALIDATION_FAILED.getCode());
    }

    @Test
    void unsafeTargetAndRunbookAreRejected() throws Exception {
        // given
        String editor = uniqueSubject("plan-editor");
        String slug = uniqueSlug("plan-unsafe");
        seedSpace(slug, editor, SpaceRole.EDITOR);
        String fileTarget = checkBody("file", "file:///etc/passwd", 60, 10);
        String scriptRunbook = """
                {"name":"runbook","type":"HTTP","target":"https://runbook.test","intervalSeconds":60,
                 "timeoutSeconds":10,"retries":1,"runbookUrl":"javascript:alert(1)","tags":[]}
                """;

        // when
        MvcResult targetResult = postCheck(editor, slug, fileTarget);
        MvcResult runbookResult = postCheck(editor, slug, scriptRunbook);
        JsonNode targetBody = jsonOf(targetResult);
        JsonNode runbookBody = jsonOf(runbookResult);

        // then
        JsonNode targetError = targetBody.path("errors").path(0);
        JsonNode runbookError = runbookBody.path("errors").path(0);
        assertThat(statusOf(targetResult)).isEqualTo(400);
        assertThat(text(targetError, "pointer")).isEqualTo("/target");
        assertThat(text(targetError, "code")).isEqualTo(CheckError.INVALID_TARGET.getCode());
        assertThat(statusOf(runbookResult)).isEqualTo(400);
        assertThat(text(runbookError, "pointer")).isEqualTo("/runbookUrl");
        assertThat(text(runbookError, "code")).isEqualTo("http_url");
    }

    private void seedSpace(String slug, String member, SpaceRole role) {
        SpaceService spaces = bean(SpaceService.class);
        spaces.put(FIXTURE_ADMIN, slug, "Plan checks", null);
        spaces.assignPlan(FIXTURE_ADMIN, slug, "limited");
        bean(SpaceMembers.class).addMember(FIXTURE_ADMIN, slug, new SpaceMember(ISSUER, member, role), false);
    }

    private MvcResult postCheck(String subject, String slug, String body) throws Exception {
        MockHttpServletRequestBuilder request = post(SPACES_PATH + "/" + slug + "/checks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(csrf())
                .with(login(subject));
        return mockMvc().perform(request).andReturn();
    }

    private static RequestPostProcessor login(String subject) {
        return oidcLogin().idToken(token -> token.subject(subject).issuer(ISSUER));
    }

    private static String checkBody(String name, String target, int intervalSeconds, int timeoutSeconds) {
        return "{\"name\":\"" + name + "\",\"type\":\"HTTP\",\"target\":\"" + target
                + "\",\"intervalSeconds\":" + intervalSeconds + ",\"timeoutSeconds\":" + timeoutSeconds
                + ",\"retries\":1,\"tags\":[]}";
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

    private static String uniqueSlug(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String uniqueSubject(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
