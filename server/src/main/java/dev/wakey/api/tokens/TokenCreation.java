package dev.wakey.api.tokens;

import dev.wakey.tokens.ApiTokens;
import dev.wakey.tokens.CreatedToken;
import dev.wakey.tokens.TokenLimitException;
import dev.wakey.tokens.TokenOptions;
import dev.wakey.tokens.TokenOwner;
import dev.wakey.web.ApiProblemException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Maps the token module's limit failure to the HTTP problem contract; the token endpoints stay thin. */
@Component
final class TokenCreation {

    private final ApiTokens tokens;

    TokenCreation(ApiTokens tokens) {
        this.tokens = tokens;
    }

    CreateTokenResponse create(TokenOwner owner, String slug, CreateTokenRequest request) {
        TokenOptions options = new TokenOptions(request.scope(), request.expiresAt());
        try {
            CreatedToken created = tokens.create(owner, slug, request.name(), options);
            return CreateTokenResponse.from(created);
        } catch (TokenLimitException exception) {
            Map<String, Object> params = Map.of("bound", exception.bound());
            throw new ApiProblemException(HttpStatus.BAD_REQUEST, "token_limit", params, exception);
        }
    }
}
