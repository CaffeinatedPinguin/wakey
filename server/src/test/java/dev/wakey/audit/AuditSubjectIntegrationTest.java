package dev.wakey.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import dev.wakey.spaces.SpaceMember;
import dev.wakey.spaces.SpaceMembers;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceService;
import dev.wakey.testsupport.SecurityIntegrationTest;
import dev.wakey.web.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** The HTTP write path against a subject of the maximum membership length. */
class AuditSubjectIntegrationTest extends SecurityIntegrationTest {

    private static final String ISSUER = "https://issuer.example.com";

    private static final CurrentUser ADMIN = new CurrentUser(ISSUER, "fixture-admin", true);

    private static final String SPACES = "/api/v1/spaces";

    @Test
    void aLongSubjectIsAudited() throws Exception {
        // given
        String subject = "s".repeat(SpaceMember.SUBJECT_MAX_LENGTH);
        String slug = uniqueSlug("long-subject");
        SpaceService spaces = bean(SpaceService.class);
        spaces.put(ADMIN, slug, "Long subject", null);
        spaces.assignPlan(ADMIN, slug, "small");
        bean(SpaceMembers.class).addMember(ADMIN, slug, new SpaceMember(ISSUER, subject, SpaceRole.EDITOR));
        UUID spaceId = spaces.view(ADMIN, slug).id();
        MockHttpServletRequestBuilder write = put(SPACES + "/" + slug + "/settings/check.interval")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":\"45\"}")
                .with(login(subject))
                .with(csrf());

        // when
        MvcResult result = mockMvc().perform(write).andReturn();
        List<AuditRecord> history =
                bean(AuditLog.class).history(spaceId, "space_setting", "check.interval", new AuditPage(null, 10));

        // then
        assertThat(statusOf(result)).isEqualTo(204);
        assertThat(history).isNotEmpty();
        AuditRecord stored = history.getFirst();
        String auditedSubject = stored.actor().subject();
        assertThat(auditedSubject).isEqualTo(subject);
    }

    private static RequestPostProcessor login(String subject) {
        return oidcLogin().idToken(token -> token.subject(subject).issuer(ISSUER));
    }

    private static int statusOf(MvcResult result) {
        return result.getResponse().getStatus();
    }

    private static String uniqueSlug(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
