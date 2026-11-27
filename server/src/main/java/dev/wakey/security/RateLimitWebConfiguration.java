package dev.wakey.security;

import dev.wakey.ratelimit.RateLimitProperties;
import dev.wakey.ratelimit.RateLimiter;
import dev.wakey.settings.SpaceSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Builds the HTTP limit filters once, so the security chains share their state. */
@Configuration(proxyBeanMethods = false)
class RateLimitWebConfiguration {

    @Bean
    RateLimitFilters rateLimitFilters(
            RateLimiter rateLimiter,
            RateLimitProperties rateLimitProperties,
            SpaceSettings spaceSettings,
            RateLimitResponse rateLimitResponse) {
        return new RateLimitFilters(rateLimiter, rateLimitProperties.login(), spaceSettings, rateLimitResponse);
    }
}
