package dev.wakey.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.web.filter.OncePerRequestFilter;

/** Invalidates a session whose absolute age exceeds the configured maximum, regardless of activity. */
@Slf4j
@RequiredArgsConstructor
final class SessionMaxAgeFilter extends OncePerRequestFilter {

    private final Clock clock;

    private final Duration maxAge;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        @Nullable HttpSession session = request.getSession(false);
        if (session != null && isOlderThanMaxAge(session)) {
            session.invalidate();
            log.info("Invalidated a session older than the maximum age of {} seconds", maxAge.toSeconds());
        }
        filterChain.doFilter(request, response);
    }

    private boolean isOlderThanMaxAge(HttpSession session) {
        long ageInMillis = clock.millis() - session.getCreationTime();
        return ageInMillis > maxAge.toMillis();
    }
}
