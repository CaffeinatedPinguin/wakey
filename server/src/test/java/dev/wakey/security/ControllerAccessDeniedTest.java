package dev.wakey.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import dev.wakey.testsupport.SecurityIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class ControllerAccessDeniedTest extends SecurityIntegrationTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Test
    void accessDeniedFromAControllerIsForbiddenNotInternalError() throws Exception {
        // given
        MockHttpServletRequestBuilder request = get("/api/v1/test-denied").with(oidcLogin());

        // when
        MvcResult result = mockMvc().perform(request).andReturn();

        // then
        String responseBody = result.getResponse().getContentAsString();
        int status = result.getResponse().getStatus();
        String contentType = result.getResponse().getContentType();
        JsonNode body = MAPPER.readTree(responseBody);
        String code = body.path("code").asString();
        assertThat(status).isEqualTo(403);
        assertThat(contentType).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(code).isEqualTo("forbidden");
    }

    @TestConfiguration
    static class DeniedControllerConfiguration {

        @Bean
        DeniedController deniedController() {
            return new DeniedController();
        }
    }

    @RestController
    static final class DeniedController {

        @GetMapping("/api/v1/test-denied")
        void denied() {
            throw new AccessDeniedException("denied by a controller");
        }
    }
}
