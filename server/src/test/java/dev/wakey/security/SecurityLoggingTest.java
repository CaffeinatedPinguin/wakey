package dev.wakey.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.wakey.audit.AuditLog;
import dev.wakey.testsupport.SecurityIntegrationTest;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.servlet.HandlerExceptionResolver;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
class SecurityLoggingTest extends SecurityIntegrationTest {

    private static final ResultMatcher UNAUTHORIZED = status().isUnauthorized();

    private static final String STRUCTURED_FORMAT = "logging.structured.format.console";

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private CapturedOutput output;

    private HandlerExceptionResolver resolver;

    @Autowired
    void setResolver(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

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
        String forgedPath = "/agent/v1/%0d%0a\r\nrequest rejected: reason=access-denied forged";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", forgedPath);
        MockHttpServletResponse response = new MockHttpServletResponse();
        UnauthorizedRequestLog entryPoint = new UnauthorizedRequestLog(resolver, mock(AuditLog.class));
        AuthenticationException exception = new InsufficientAuthenticationException("n/a");

        // when
        entryPoint.commence(request, response, exception);

        // then
        List<String> entries = logEntries("request rejected:");
        String captured = output.getOut();
        int status = response.getStatus();
        assertThat(status).isEqualTo(401);
        assertThat(entries).hasSize(1);
        assertThat(captured).doesNotContain("\r\nrequest rejected:").doesNotContain("\nrequest rejected:");
        assertThat(entries.getFirst()).contains("path=/agent/v1/%0d%0a");
    }

    @Test
    void structuredLogsKeepEachEntryOnOneJsonLine() throws Exception {
        // given
        ConfigurableEnvironment environment = bean(ConfigurableEnvironment.class);
        LoggingSystem loggingSystem = bean(LoggingSystem.class);
        MapPropertySource structuredFormat =
                new MapPropertySource("structured-console", Map.of(STRUCTURED_FORMAT, "ecs"));
        String forgedPath = "/agent/v1/%0d%0a\r\nrequest rejected: reason=access-denied forged";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", forgedPath);
        MockHttpServletResponse response = new MockHttpServletResponse();
        UnauthorizedRequestLog entryPoint = new UnauthorizedRequestLog(resolver, mock(AuditLog.class));
        AuthenticationException exception = new InsufficientAuthenticationException("n/a");

        // when
        environment.getPropertySources().addFirst(structuredFormat);
        try {
            reinitializeLogging(loggingSystem, environment);
            entryPoint.commence(request, response, exception);
        } finally {
            environment.getPropertySources().remove(structuredFormat.getName());
            reinitializeLogging(loggingSystem, environment);
        }

        // then
        List<String> entries = logEntries("request rejected:");
        int status = response.getStatus();
        assertThat(status).isEqualTo(401);
        assertThat(entries).hasSize(1);
        JsonNode entry = MAPPER.readTree(entries.getFirst());
        String message = entry.get("message").asText();
        assertThat(message).contains("/agent/v1/");
    }

    private static void reinitializeLogging(LoggingSystem loggingSystem, ConfigurableEnvironment environment) {
        LoggingInitializationContext context = new LoggingInitializationContext(environment);
        loggingSystem.cleanUp();
        loggingSystem.initialize(context, null, null);
    }

    private List<String> logEntries(String fragment) {
        Stream<String> lines = output.getOut().lines();
        return lines.filter(line -> line.contains(fragment)).toList();
    }
}
