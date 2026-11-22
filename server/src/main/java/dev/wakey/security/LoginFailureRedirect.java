package dev.wakey.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/** Sends a failed login back to the frontend with one fixed marker and no error details. */
@Slf4j
final class LoginFailureRedirect implements AuthenticationFailureHandler {

    private static final String LOGIN_FAILED = "login=failed";

    private final String redirectUrl;

    LoginFailureRedirect(String frontendUrl) {
        this.redirectUrl = failureUrl(frontendUrl);
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        String reason = exception.getClass().getSimpleName();
        log.warn("login failed: {}", reason);
        invalidateSession(request);
        response.sendRedirect(redirectUrl);
    }

    private static String failureUrl(String frontendUrl) {
        String separator = frontendUrl.contains("?") ? "&" : "?";
        return frontendUrl + separator + LOGIN_FAILED;
    }

    private static void invalidateSession(HttpServletRequest request) {
        @Nullable HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
