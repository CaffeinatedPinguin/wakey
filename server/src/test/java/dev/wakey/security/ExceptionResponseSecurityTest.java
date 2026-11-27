package dev.wakey.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import dev.wakey.testsupport.SecurityIntegrationTest;
import dev.wakey.web.CommonError;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class ExceptionResponseSecurityTest extends SecurityIntegrationTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Test
    void unauthenticatedResponseIsErrorJson() throws Exception {
        // when
        MvcResult result = mockMvc().perform(get("/api/v1/me")).andReturn();

        // then
        int status = result.getResponse().getStatus();
        String rawContentType = result.getResponse().getContentType();
        String contentType = Objects.requireNonNull(rawContentType);
        String responseBody = result.getResponse().getContentAsString();
        JsonNode body = MAPPER.readTree(responseBody);
        String code = body.path("code").asString();
        assertThat(status).isEqualTo(401);
        assertThat(contentType).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(code).isEqualTo(CommonError.UNAUTHENTICATED.getCode());
    }

    @Test
    void forbiddenResponseIsErrorJson() throws Exception {
        // given
        var request = post("/logout").with(oidcLogin());

        // when
        MvcResult result = mockMvc().perform(request).andReturn();

        // then
        int status = result.getResponse().getStatus();
        String rawContentType = result.getResponse().getContentType();
        String contentType = Objects.requireNonNull(rawContentType);
        String responseBody = result.getResponse().getContentAsString();
        JsonNode body = MAPPER.readTree(responseBody);
        String code = body.path("code").asString();
        assertThat(status).isEqualTo(403);
        assertThat(contentType).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(code).isEqualTo(CommonError.FORBIDDEN.getCode());
    }

    @Test
    void securityExceptionsUseTheSameShapeAsMvcExceptions() throws Exception {
        // given
        MockHttpServletRequestBuilder unauthenticatedRequest = get("/api/v1/me");
        MockHttpServletRequestBuilder missingResourceRequest =
                get("/api/v1/does-not-exist").with(oidcLogin());

        // when
        MvcResult unauthenticated = mockMvc().perform(unauthenticatedRequest).andReturn();
        MvcResult missingResource = mockMvc().perform(missingResourceRequest).andReturn();

        // then
        int unauthenticatedStatus = unauthenticated.getResponse().getStatus();
        int missingResourceStatus = missingResource.getResponse().getStatus();
        Set<String> securityFields = fieldNamesOf(unauthenticated);
        Set<String> mvcFields = fieldNamesOf(missingResource);
        assertThat(unauthenticatedStatus).isEqualTo(401);
        assertThat(missingResourceStatus).isEqualTo(404);
        assertThat(securityFields).containsExactlyInAnyOrderElementsOf(mvcFields);
        assertThat(securityFields).contains("type", "title", "status", "detail", "code", "errors");
    }

    private static Set<String> fieldNamesOf(MvcResult result) throws Exception {
        String responseBody = result.getResponse().getContentAsString();
        JsonNode body = MAPPER.readTree(responseBody);
        Set<String> fields = new LinkedHashSet<>();
        for (String name : body.propertyNames()) {
            fields.add(name);
        }
        return fields;
    }
}
