package dev.wakey.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** The instance limits: the remembered keys and the login-attempt bucket of one client address. */
@ConfigurationProperties(prefix = "wakey.rate-limit")
public record RateLimitProperties(int maxKeys, Login login) {

    public RateLimitProperties {
        if (maxKeys < 1) {
            throw new IllegalArgumentException("maxKeys must be at least 1");
        }
    }

    /** One token bucket per client address at the OIDC login entry points. */
    public record Login(int capacity, double refillPerSecond) {

        public Login {
            if (capacity < 1) {
                throw new IllegalArgumentException("capacity must be at least 1");
            }
            if (!Double.isFinite(refillPerSecond) || refillPerSecond <= 0) {
                throw new IllegalArgumentException("refillPerSecond must be a positive finite number");
            }
        }
    }
}
