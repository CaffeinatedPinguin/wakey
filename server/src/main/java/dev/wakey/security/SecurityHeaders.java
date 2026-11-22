package dev.wakey.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

/** The security headers applied to every filter chain. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class SecurityHeaders {

    private static final String CONTENT_SECURITY_POLICY = "default-src 'none'; frame-ancestors 'none'";

    private static final String PERMISSIONS_POLICY = "camera=(), microphone=(), geolocation=()";

    static void applyTo(HeadersConfigurer<HttpSecurity> headers) {
        headers.contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY));
        headers.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER));
        headers.permissionsPolicyHeader(permissions -> permissions.policy(PERMISSIONS_POLICY));
    }
}
