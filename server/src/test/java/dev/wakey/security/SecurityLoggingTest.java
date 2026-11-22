package dev.wakey.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.wakey.testsupport.SecurityIntegrationTest;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

@ExtendWith(OutputCaptureExtension.class)
class SecurityLoggingTest extends SecurityIntegrationTest {

    private static final ResultMatcher UNAUTHORIZED = status().isUnauthorized();

    private CapturedOutput output;

    @BeforeEach
    void captureOutput(CapturedOutput output) {
        this.output = output;
    }

    @Test
    void unauthenticatedRequestIsLoggedWithReason() throws Exception {
        // when
        MvcResult result = mockMvc()
                .perform(get("/agent/v1/checks"))
                .andExpect(UNAUTHORIZED)
                .andReturn();

        // then
        List<String> entries =
                logEntries("request rejected: reason=missing-authentication method=GET path=/agent/v1/checks");
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(401);
        assertThat(entries).hasSize(1);
        assertThat(entries.getFirst()).contains("INFO");
    }

    @Test
    void forbiddenRequestIsLoggedWithReason() throws Exception {
        // given
        var request = post("/logout").with(oidcLogin());

        // when
        MvcResult result = mockMvc().perform(request).andReturn();

        // then
        List<String> entries = logEntries("request rejected: reason=csrf-token-missing method=POST path=/logout");
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(403);
        assertThat(entries).hasSize(1);
        assertThat(entries.getFirst()).contains("WARN");
    }

    @Test
    void controlCharactersInThePathCannotForgeALogLine() throws Exception {
        // given
        String forgedPath = "/agent/v1/%0d%0aforged:request-rejected-reason=access-denied";
        // StrictHttpFirewall rejects encoded control characters before the chain reaches the entry point.
        MockHttpServletRequest request = new MockHttpServletRequest("GET", forgedPath);
        MockHttpServletResponse response = new MockHttpServletResponse();
        UnauthorizedRequestLog entryPoint = new UnauthorizedRequestLog();
        AuthenticationException exception = new InsufficientAuthenticationException("n/a");

        // when
        entryPoint.commence(request, response, exception);

        // then
        List<String> entries = logEntries("request rejected:");
        String captured = output.getOut();
        int status = response.getStatus();
        assertThat(status).isEqualTo(401);
        assertThat(entries).hasSize(1);
        assertThat(captured)
                .contains("path=/agent/v1/\\r\\nforged:request-rejected")
                .doesNotContain("\r\nforged");
    }

    private List<String> logEntries(String fragment) {
        Stream<String> lines = output.getOut().lines();
        return lines.filter(line -> line.contains(fragment)).toList();
    }
}
