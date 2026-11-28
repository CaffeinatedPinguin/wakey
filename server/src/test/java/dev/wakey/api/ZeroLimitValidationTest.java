package dev.wakey.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import dev.wakey.spaces.SpaceService;
import dev.wakey.testsupport.SecurityIntegrationTest;
import dev.wakey.web.CommonError;
import dev.wakey.web.CurrentUser;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** A non-positive or non-numeric page size is the same field validation problem on every list. */
class ZeroLimitValidationTest extends SecurityIntegrationTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static final String ISSUER = "https://issuer.example.com";

    private static final String ADMIN_ROLE = "ROLE_ADMIN";

    private static final String SUBJECT = "zero-limit-admin";

    private static final CurrentUser ADMIN = new CurrentUser(ISSUER, "fixture-admin", true);

    @Test
    void aZeroLimitIsAValidationProblemOnEveryList() throws Exception {
        // given
        String slug = uniqueSlug();
        bean(SpaceService.class).put(ADMIN, slug, "Zero limit", null);
        List<String> paths =
                List.of("/api/v1/spaces", "/api/v1/spaces/" + slug + "/members", "/api/v1/spaces/" + slug + "/checks");

        // when
        assertValidationProblem(paths, "0");
        assertValidationProblem(paths, "abc");

        // then every list answered a validation problem; the helper asserts each answer
    }

    private void assertValidationProblem(List<String> paths, String limit) throws Exception {
        for (String path : paths) {
            MockHttpServletRequestBuilder request =
                    get(path).param("limit", limit).with(admin());
            MvcResult result = mockMvc().perform(request).andReturn();
            JsonNode body = jsonOf(result);
            List<String> pointers = pointersOf(body);
            int status = result.getResponse().getStatus();
            String contentType = result.getResponse().getContentType();
            String code = body.path("code").asString();
            String label = path + " limit=" + limit;

            // then
            assertThat(status).as(label).isEqualTo(400);
            assertThat(contentType).as(label).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            assertThat(code).as(label).isEqualTo(CommonError.VALIDATION_FAILED.getCode());
            assertThat(pointers).as(label).contains("/limit");
        }
    }

    private static RequestPostProcessor admin() {
        return oidcLogin()
                .idToken(token -> token.subject(SUBJECT).issuer(ISSUER))
                .authorities(new SimpleGrantedAuthority(ADMIN_ROLE));
    }

    private static JsonNode jsonOf(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        return MAPPER.readTree(content);
    }

    private static List<String> pointersOf(JsonNode body) {
        List<String> pointers = new ArrayList<>();
        for (JsonNode error : body.path("errors")) {
            String pointer = error.path("pointer").asString();
            pointers.add(pointer);
        }
        return pointers;
    }

    private static String uniqueSlug() {
        return "zero-limit-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
