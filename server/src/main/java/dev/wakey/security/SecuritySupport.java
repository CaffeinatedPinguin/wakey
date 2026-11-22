package dev.wakey.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.HeaderWriterLogoutHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter.Directive;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/** The headers, logout and login-redirect helpers shared by the security configuration. */
@Slf4j
final class SecuritySupport {

    private static final String LOGIN_FAILED = "login=failed";

    private static final String CONTENT_SECURITY_POLICY = "default-src 'none'; frame-ancestors 'none'";

    private static final String PERMISSIONS_POLICY = "camera=(), microphone=(), geolocation=()";

    private SecuritySupport() {}

    static void applyHeaders(HeadersConfigurer<HttpSecurity> headers) {
        headers.contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY));
        headers.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER));
        headers.permissionsPolicyHeader(permissions -> permissions.policy(PERMISSIONS_POLICY));
    }

    static void configureLogout(LogoutConfigurer<HttpSecurity> logout) {
        logout.logoutRequestMatcher(PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/logout"))
                .addLogoutHandler(new HeaderWriterLogoutHandler(new ClearSiteDataHeaderWriter(Directive.COOKIES)))
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler());
    }

    static AuthenticationFailureHandler failureHandler(String frontendUrl) {
        String redirectUrl = failureUrl(frontendUrl);
        return (request, response, exception) -> {
            String reason = exception.getClass().getSimpleName();
            log.warn("login failed: {}", reason);
            invalidateSession(request);
            response.sendRedirect(redirectUrl);
        };
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

/** Invalidates a session whose absolute age exceeds the configured maximum, regardless of activity. */
final class SessionMaxAgeFilter extends OncePerRequestFilter {

    private final Clock clock;

    private final Duration maxAge;

    SessionMaxAgeFilter(Clock clock, Duration maxAge) {
        this.clock = clock;
        this.maxAge = maxAge;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        @Nullable HttpSession session = request.getSession(false);
        if (session != null && isOlderThanMaxAge(session)) {
            session.invalidate();
        }
        filterChain.doFilter(request, response);
    }

    private boolean isOlderThanMaxAge(HttpSession session) {
        long ageInMillis = clock.millis() - session.getCreationTime();
        return ageInMillis > maxAge.toMillis();
    }
}
