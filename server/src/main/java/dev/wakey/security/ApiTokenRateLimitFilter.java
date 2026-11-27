package dev.wakey.security;

import dev.wakey.ratelimit.Decision;
import dev.wakey.ratelimit.RateLimit;
import dev.wakey.ratelimit.RateLimiter;
import dev.wakey.settings.SpacePlan;
import dev.wakey.settings.SpaceSettings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Limits token API requests per space by the plan's requests-per-minute value. */
@Slf4j
final class ApiTokenRateLimitFilter extends OncePerRequestFilter {

    private static final String KEY_PREFIX = "space:";

    private static final String REASON = "plan-limit";

    private static final double SECONDS_PER_MINUTE = 60.0;

    private static final Decision ALLOWED = new Decision(true, Duration.ZERO);

    private final RateLimiter limiter;

    private final SpaceSettings settings;

    private final RateLimitResponse rejections;

    ApiTokenRateLimitFilter(RateLimiter limiter, SpaceSettings settings, RateLimitResponse rejections) {
        this.limiter = limiter;
        this.settings = settings;
        this.rejections = rejections;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof ApiTokenAuthentication token && rejectWhenOverLimit(request, response, token)) {
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean rejectWhenOverLimit(
            HttpServletRequest request, HttpServletResponse response, ApiTokenAuthentication token) {
        UUID spaceId = token.identity().spaceId();
        SpacePlan plan = settings.planOf(spaceId);
        Decision decision = decide(spaceId, plan);
        boolean denied = !decision.allowed();
        if (denied) {
            String spaceSlug = token.identity().spaceSlug();
            String path = request.getRequestURI();
            log.warn(
                    "api rate limit exceeded: reason={} space={} plan={} path={}",
                    REASON,
                    spaceSlug,
                    plan.name(),
                    path);
            rejections.reject(request, response, decision.retryAfter());
        }
        return denied;
    }

    private Decision decide(UUID spaceId, SpacePlan plan) {
        @Nullable RateLimit limit = limitOf(plan);
        return limit == null ? ALLOWED : limiter.tryAcquire(key(spaceId), limit);
    }

    private static String key(UUID spaceId) {
        return KEY_PREFIX + spaceId;
    }

    private static @Nullable RateLimit limitOf(SpacePlan plan) {
        Integer requestsPerMinute = plan.properties().apiRequestsPerMinute();
        if (requestsPerMinute == null) {
            return null;
        }
        return new RateLimit(requestsPerMinute, requestsPerMinute / SECONDS_PER_MINUTE);
    }
}
