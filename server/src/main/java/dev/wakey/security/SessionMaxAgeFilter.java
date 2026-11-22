package dev.wakey.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.springframework.web.filter.OncePerRequestFilter;

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
