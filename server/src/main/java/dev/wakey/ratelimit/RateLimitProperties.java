package dev.wakey.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Bounds the number of remembered keys so the in-memory limiter cannot grow without limit. */
@ConfigurationProperties(prefix = "wakey.ratelimit")
public record RateLimitProperties(@DefaultValue("10000") int maxKeys) {

    public RateLimitProperties {
        if (maxKeys < 1) {
            throw new IllegalArgumentException("maxKeys must be at least 1");
        }
    }
}
