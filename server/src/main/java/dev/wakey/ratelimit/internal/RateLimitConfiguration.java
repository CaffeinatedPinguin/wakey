package dev.wakey.ratelimit.internal;

import dev.wakey.ratelimit.RateLimitProperties;
import dev.wakey.ratelimit.RateLimiter;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RateLimitProperties.class)
class RateLimitConfiguration {

    @Bean
    RateLimiter rateLimiter(RateLimitProperties properties, Clock clock) {
        return new TokenBucketRateLimiter(properties.maxKeys(), clock);
    }
}
