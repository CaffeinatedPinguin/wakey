package dev.wakey.security;

import dev.wakey.web.ProblemDetailsWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** Logs every 401 with a fixed reason and returns an RFC 9457 Problem Details body. */
@Slf4j
final class UnauthorizedRequestLog implements AuthenticationEntryPoint {

    private static final String MISSING_AUTHENTICATION = "missing-authentication";

    private static final String PROBLEM_JSON = "application/problem+json";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        String method = request.getMethod();
        String path = request.getRequestURI();
        log.info("request rejected: reason={} method={} path={}", MISSING_AUTHENTICATION, method, path);
        if (response.isCommitted()) {
            return;
        }
        response.setContentType(PROBLEM_JSON);
        ProblemDetailsWriter.write(response, HttpStatus.UNAUTHORIZED, "unauthenticated", "Authentication is required.");
    }
}
