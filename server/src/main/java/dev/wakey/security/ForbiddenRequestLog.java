package dev.wakey.security;

import dev.wakey.web.ProblemDetailsWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;

/** Logs every 403 with a fixed reason and returns an RFC 9457 Problem Details body, including CSRF rejections. */
@Slf4j
final class ForbiddenRequestLog implements AccessDeniedHandler {

    private static final String ACCESS_DENIED = "access-denied";

    private static final String CSRF_TOKEN_MISSING = "csrf-token-missing";

    private static final String CSRF_TOKEN_INVALID = "csrf-token-invalid";

    private static final String PROBLEM_JSON = "application/problem+json";

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException {
        String reason = reasonFor(exception);
        String method = request.getMethod();
        String path = request.getRequestURI();
        log.warn("request rejected: reason={} method={} path={}", reason, method, path);
        if (response.isCommitted()) {
            return;
        }
        response.setContentType(PROBLEM_JSON);
        ProblemDetailsWriter.write(response, HttpStatus.FORBIDDEN, "forbidden", "Access is denied.");
    }

    private static String reasonFor(AccessDeniedException exception) {
        String reason = ACCESS_DENIED;
        if (exception instanceof MissingCsrfTokenException) {
            reason = CSRF_TOKEN_MISSING;
        } else if (exception instanceof InvalidCsrfTokenException) {
            reason = CSRF_TOKEN_INVALID;
        }
        return reason;
    }
}
