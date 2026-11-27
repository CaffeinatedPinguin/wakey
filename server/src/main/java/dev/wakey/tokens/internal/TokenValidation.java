package dev.wakey.tokens.internal;

import dev.wakey.tokens.TokenError;
import dev.wakey.web.ApiException;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/** The token field rules, shared by the token store without widening its dependencies. */
public final class TokenValidation {

    private TokenValidation() {}

    public static void requireFutureExpiry(@Nullable Instant expiresAt, Instant now) {
        if (expiresAt != null && !expiresAt.isAfter(now)) {
            throw ApiException.invalidField(TokenError.INVALID_EXPIRY, "/expiresAt");
        }
    }
}
