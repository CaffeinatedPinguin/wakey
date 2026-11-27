package dev.wakey.security;

import dev.wakey.ratelimit.Decision;
import dev.wakey.ratelimit.RateLimit;
import dev.wakey.ratelimit.RateLimiter;
import dev.wakey.web.ApiPaths;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/** Limits the OIDC login entry points per client address; a denied attempt is a 429. */
@Slf4j
final class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final RequestMatcher LOGIN_ATTEMPTS = new OrRequestMatcher(
            PathPatternRequestMatcher.pathPattern(ApiPaths.OAUTH2 + "/authorization/**"),
            PathPatternRequestMatcher.pathPattern(ApiPaths.LOGIN + "/oauth2/code/**"));

    private static final String KEY_PREFIX = "login:";

    private static final String REASON = "too-many-attempts";

    private final RateLimiter limiter;

    private final RateLimit limit;

    private final RateLimitResponse rejections;

    LoginRateLimitFilter(RateLimiter limiter, RateLimit limit, RateLimitResponse rejections) {
        this.limiter = limiter;
        this.limit = limit;
        this.rejections = rejections;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !LOGIN_ATTEMPTS.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String address = request.getRemoteAddr();
        Decision decision = limiter.tryAcquire(KEY_PREFIX + address, limit);
        if (!decision.allowed()) {
            log.warn(
                    "login rate limit exceeded: reason={} address={} path={}",
                    REASON,
                    address,
                    request.getRequestURI());
            rejections.reject(request, response, decision.retryAfter());
            return;
        }
        chain.doFilter(request, response);
    }
}
