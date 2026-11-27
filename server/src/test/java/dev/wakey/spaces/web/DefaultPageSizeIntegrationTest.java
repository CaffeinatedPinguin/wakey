package dev.wakey.spaces.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import dev.wakey.security.CurrentUser;
import dev.wakey.spaces.SpaceService;
import dev.wakey.testsupport.SecurityIntegrationTest;
import dev.wakey.web.ApiProperties;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@TestPropertySource(properties = "wakey.api.default-page-size=3")
class DefaultPageSizeIntegrationTest extends SecurityIntegrationTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static final String SPACES_PATH = "/api/v1/spaces";

    private static final String ADMIN_ROLE = "ROLE_ADMIN";

    @Test
    void theDefaultPageSizeComesFromTheConfiguration() throws Exception {
        // given
        String admin = uniqueSubject("default-page-size");
        String prefix = uniqueSlug("default-page-size");
        for (int index = 0; index < 5; index++) {
            seedSpace(prefix + "-" + index);
        }
        int configured = bean(ApiProperties.class).defaultPageSize();
        MockHttpServletRequestBuilder request = get(SPACES_PATH).with(adminLogin(admin));

        // when
        MvcResult result = mockMvc().perform(request).andReturn();

        // then
        String content = result.getResponse().getContentAsString();
        JsonNode body = MAPPER.readTree(content);
        List<JsonNode> items = body.path("items").valueStream().toList();
        int status = result.getResponse().getStatus();
        boolean nextCursorIsNull = body.path("nextCursor").isNull();
        assertThat(status).isEqualTo(200);
        assertThat(configured).isEqualTo(3);
        assertThat(items).hasSize(configured);
        assertThat(nextCursorIsNull).isFalse();
    }

    private static RequestPostProcessor adminLogin(String subject) {
        return oidcLogin()
                .idToken(token -> token.subject(subject).issuer("https://issuer.example.com"))
                .authorities(new SimpleGrantedAuthority(ADMIN_ROLE));
    }

    private void seedSpace(String slug) {
        CurrentUser admin = new CurrentUser("https://issuer.example.com", "fixture-admin", true);
        bean(SpaceService.class).put(admin, slug, "Default page space", null);
    }

    private static String uniqueSlug(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String uniqueSubject(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
