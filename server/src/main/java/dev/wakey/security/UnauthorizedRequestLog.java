package dev.wakey.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** Logs every 401 with a fixed reason and the sanitized method and path, then returns the status alone. */
@Slf4j
final class UnauthorizedRequestLog implements AuthenticationEntryPoint {

    private static final String MISSING_AUTHENTICATION = "missing-authentication";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        String method = LogSafeText.sanitize(request.getMethod());
        String path = LogSafeText.sanitize(request.getRequestURI());
        log.info("request rejected: reason={} method={} path={}", MISSING_AUTHENTICATION, method, path);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
