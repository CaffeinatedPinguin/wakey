package dev.wakey.checks.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import dev.wakey.audit.AuditActor;
import dev.wakey.checks.CheckDraft;
import dev.wakey.checks.CheckService;
import dev.wakey.checks.CheckType;
import dev.wakey.spaces.PutResult;
import dev.wakey.spaces.Space;
import dev.wakey.spaces.SpaceMember;
import dev.wakey.spaces.SpaceMembers;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceService;
import dev.wakey.testsupport.SecurityIntegrationTest;
import dev.wakey.web.ApiProperties;
import dev.wakey.web.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class ChecksDefaultPageSizeIntegrationTest extends SecurityIntegrationTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static final String SPACES_PATH = "/api/v1/spaces";

    private static final String ISSUER = "https://issuer.example.com";

    private static final CurrentUser FIXTURE_ADMIN = new CurrentUser(ISSUER, "fixture-admin", true);

    private static final AuditActor ACTOR = AuditActor.of("checks-default-page-test", false);

    @Test
    void theCheckListUsesTheConfiguredDefaultPageSize() throws Exception {
        // given
        String editor = uniqueSubject("editor");
        String slug = uniqueSlug("default-size");
        Space space = seedSpace(slug, editor);
        int configured = bean(ApiProperties.class).defaultPageSize();
        for (int index = 0; index <= configured; index++) {
            seedCheck(space, "check-" + index);
        }
        MockHttpServletRequestBuilder request =
                get(SPACES_PATH + "/" + slug + "/checks").with(login(editor));

        // when
        MvcResult result = mockMvc().perform(request).andReturn();

        // then
        JsonNode body = jsonOf(result);
        List<JsonNode> items = body.path("items").valueStream().toList();
        boolean nextCursorIsNull = body.path("nextCursor").isNull();
        assertThat(statusOf(result)).isEqualTo(200);
        assertThat(items).hasSize(configured);
        assertThat(nextCursorIsNull).isFalse();
    }

    private Space seedSpace(String slug, String member) {
        SpaceService spaces = bean(SpaceService.class);
        PutResult put = spaces.put(FIXTURE_ADMIN, slug, "Checks", null);
        bean(SpaceMembers.class)
                .addMember(FIXTURE_ADMIN, slug, new SpaceMember(ISSUER, member, SpaceRole.EDITOR), false);
        return put.space();
    }

    private void seedCheck(Space space, String name) {
        String target = "https://" + name + ".test";
        CheckDraft draft = new CheckDraft(name, CheckType.HTTP, target, 60, 10, 1, null, null, null, List.of());
        bean(CheckService.class).create(space.id(), draft, ACTOR);
    }

    private static RequestPostProcessor login(String subject) {
        return oidcLogin().idToken(token -> token.subject(subject).issuer(ISSUER));
    }

    private static int statusOf(MvcResult result) {
        return result.getResponse().getStatus();
    }

    private static JsonNode jsonOf(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        return MAPPER.readTree(content);
    }

    private static String uniqueSlug(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String uniqueSubject(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
