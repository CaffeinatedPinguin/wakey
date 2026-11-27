package dev.wakey.security;

import dev.wakey.ratelimit.RateLimit;
import dev.wakey.ratelimit.RateLimitProperties;
import dev.wakey.ratelimit.RateLimiter;
import dev.wakey.settings.SpaceSettings;

/** The two HTTP limits over the shared in-memory limiter; the filter chain installs them once. */
final class RateLimitFilters {

    private final LoginRateLimitFilter login;

    private final ApiTokenRateLimitFilter apiTokens;

    RateLimitFilters(
            RateLimiter limiter,
            RateLimitProperties.Login login,
            SpaceSettings settings,
            RateLimitResponse rejections) {
        RateLimit loginLimit = new RateLimit(login.capacity(), login.refillPerSecond());
        this.login = new LoginRateLimitFilter(limiter, loginLimit, rejections);
        this.apiTokens = new ApiTokenRateLimitFilter(limiter, settings, rejections);
    }

    LoginRateLimitFilter loginFilter() {
        return login;
    }

    ApiTokenRateLimitFilter apiTokenFilter() {
        return apiTokens;
    }
}
