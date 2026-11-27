package dev.wakey.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.servlet.HandlerExceptionResolver;

/** A rejection no resolver accepts must still answer 401/403 instead of falling back to 200. */
@ExtendWith(OutputCaptureExtension.class)
class SecurityRejectionStatusTest {

    private static final HandlerExceptionResolver UNHANDLED = (request, response, handler, exception) -> null;

    private CapturedOutput output;

    @BeforeEach
    void captureOutput(CapturedOutput output) {
        this.output = output;
    }

    @Test
    void anUnhandledUnauthenticatedRejectionStillAnswers401() {
        // given
        UnauthorizedRequestLog entryPoint = new UnauthorizedRequestLog(UNHANDLED);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/agent/v1/checks");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException exception = new InsufficientAuthenticationException("n/a");

        // when
        entryPoint.commence(request, response, exception);

        // then
        int status = response.getStatus();
        assertThat(status).isEqualTo(401);
        assertThat(logEntries(output, "reason=missing-authentication")).hasSize(1);
    }

    @Test
    void anUnhandledForbiddenRejectionStillAnswers403() {
        // given
        ForbiddenRequestLog handler = new ForbiddenRequestLog(UNHANDLED);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/logout");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AccessDeniedException exception = new AccessDeniedException("n/a");

        // when
        handler.handle(request, response, exception);

        // then
        int status = response.getStatus();
        assertThat(status).isEqualTo(403);
        assertThat(logEntries(output, "reason=access-denied")).hasSize(1);
    }

    private static List<String> logEntries(CapturedOutput output, String fragment) {
        return output.getOut().lines().filter(line -> line.contains(fragment)).toList();
    }
}
