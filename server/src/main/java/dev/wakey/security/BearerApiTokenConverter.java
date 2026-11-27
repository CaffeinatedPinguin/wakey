package dev.wakey.security;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;

/**
 * Reads an {@code Authorization: Bearer ...} secret. A request without that header is left to the
 * session; a present but empty secret is still authenticated (and rejected) as a token.
 */
final class BearerApiTokenConverter implements AuthenticationConverter {

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public @Nullable Authentication convert(HttpServletRequest request) {
        @Nullable String secret = bearerSecret(request);
        return secret == null ? null : new ApiTokenRequest(secret);
    }

    static boolean hasBearerSecret(HttpServletRequest request) {
        return bearerSecret(request) != null;
    }

    private static @Nullable String bearerSecret(HttpServletRequest request) {
        @Nullable String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        return header.substring(BEARER_PREFIX.length()).trim();
    }
}
