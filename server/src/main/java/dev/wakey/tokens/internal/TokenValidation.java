package dev.wakey.tokens.internal;

import dev.wakey.spaces.SpaceError;
import dev.wakey.tokens.TokenError;
import dev.wakey.web.ApiException;
import dev.wakey.web.ApiFieldError;
import dev.wakey.web.CommonError;
import dev.wakey.web.DisplayText;
import dev.wakey.web.ErrorCode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** The token field rules, shared by the token store without widening its dependencies. */
public final class TokenValidation {

    private static final int MAX_NAME_LENGTH = 100;

    private TokenValidation() {}

    public static String requireValidName(String name) {
        String normalized = DisplayText.normalize(name);
        if (!DisplayText.isValid(normalized, MAX_NAME_LENGTH, false)) {
            throw invalid(SpaceError.INVALID_NAME, "name");
        }
        return normalized;
    }

    public static void requireFutureExpiry(@Nullable Instant expiresAt, Instant now) {
        if (expiresAt != null && !expiresAt.isAfter(now)) {
            throw invalid(TokenError.INVALID_EXPIRY, "expiresAt");
        }
    }

    private static ApiException invalid(ErrorCode code, String field) {
        ApiFieldError problem = new ApiFieldError("/" + field, code.getCode(), Map.of());
        return new ApiException(CommonError.VALIDATION_FAILED, List.of(problem));
    }
}
