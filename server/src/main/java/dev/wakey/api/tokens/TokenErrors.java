package dev.wakey.api.tokens;

import dev.wakey.security.CurrentUser;
import dev.wakey.spaces.SpaceAccessDeniedException;
import dev.wakey.spaces.SpaceNotFoundException;
import dev.wakey.tokens.ApiTokens;
import dev.wakey.tokens.CreatedToken;
import dev.wakey.tokens.InvalidTokenException;
import dev.wakey.tokens.TokenOptions;
import dev.wakey.tokens.TokenOwner;
import dev.wakey.tokens.TokenSummary;
import dev.wakey.web.ApiProblemException;
import dev.wakey.web.Problems;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

/** Turns the tokens module's domain errors into the HTTP Problem Details contract. */
final class TokenErrors {

    private TokenErrors() {}

    static CreateTokenResponse create(
            ApiTokens tokens, Authentication authentication, String slug, CreateTokenRequest request) {
        TokenOwner owner = owner(authentication);
        TokenOptions options = new TokenOptions(request.scope(), request.expiresAt());
        CreatedToken created = mapped(() -> tokens.create(owner, slug, request.name(), options));
        return CreateTokenResponse.from(created);
    }

    static List<TokenSummary> list(ApiTokens tokens, Authentication authentication, String slug) {
        TokenOwner owner = owner(authentication);
        return mapped(() -> tokens.list(owner, slug));
    }

    static void revoke(ApiTokens tokens, Authentication authentication, String slug, UUID id) {
        TokenOwner owner = owner(authentication);
        mapped(() -> tokens.revoke(owner, slug, id));
    }

    private static TokenOwner owner(Authentication authentication) {
        CurrentUser user = CurrentUser.from(authentication);
        return new TokenOwner(user.subject());
    }

    static <T> T mapped(Supplier<T> action) {
        try {
            return action.get();
        } catch (SpaceNotFoundException | SpaceAccessDeniedException | InvalidTokenException exception) {
            throw translate(exception);
        }
    }

    static void mapped(Runnable action) {
        try {
            action.run();
        } catch (SpaceNotFoundException | SpaceAccessDeniedException | InvalidTokenException exception) {
            throw translate(exception);
        }
    }

    private static ApiProblemException translate(RuntimeException exception) {
        ApiProblemException translated;
        if (exception instanceof SpaceNotFoundException) {
            translated = new ApiProblemException(HttpStatus.NOT_FOUND, "not_found", exception);
        } else if (exception instanceof SpaceAccessDeniedException) {
            translated = new ApiProblemException(HttpStatus.FORBIDDEN, "forbidden", exception);
        } else {
            InvalidTokenException invalid = (InvalidTokenException) exception;
            Map<String, Object> error = Problems.error("/" + invalid.field(), invalid.code());
            translated =
                    new ApiProblemException(HttpStatus.BAD_REQUEST, "validation_failed", List.of(error), exception);
        }
        return translated;
    }
}
