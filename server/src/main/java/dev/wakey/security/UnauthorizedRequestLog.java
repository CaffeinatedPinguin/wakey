package dev.wakey.security;

import dev.wakey.web.ProblemDetailsWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** Logs every 401 with a fixed reason and returns an RFC 9457 Problem Details body. */
@Slf4j
final class UnauthorizedRequestLog implements AuthenticationEntryPoint {

    private static final String MISSING_AUTHENTICATION = "missing-authentication";

    private static final String INVALID_TOKEN = "invalid-token";

    private static final String INVALID_TOKEN_DETAIL = "The API token is invalid.";

    private static final String INVALID_TOKEN_CHALLENGE = "Bearer error=\"invalid_token\"";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        logRejected(MISSING_AUTHENTICATION, request);
        if (response.isCommitted()) {
            return;
        }
        ProblemDetailsWriter.write(response, HttpStatus.UNAUTHORIZED, "unauthenticated", "Authentication is required.");
    }

    /** Rejects a presented token with the RFC 6750 challenge and a closed-list reason. */
    static void rejectInvalidToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        logRejected(INVALID_TOKEN, request);
        if (response.isCommitted()) {
            return;
        }
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, INVALID_TOKEN_CHALLENGE);
        ProblemDetailsWriter.write(response, HttpStatus.UNAUTHORIZED, "invalid_token", INVALID_TOKEN_DETAIL);
    }

    private static void logRejected(String reason, HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        log.info("request rejected: reason={} method={} path={}", reason, method, path);
    }
}
