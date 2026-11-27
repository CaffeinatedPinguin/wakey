package dev.wakey.tokens.web;

import dev.wakey.spaces.web.SessionUser;
import dev.wakey.tokens.ApiTokens;
import dev.wakey.tokens.CreatedToken;
import dev.wakey.tokens.TokenOptions;
import dev.wakey.tokens.TokenOwner;
import dev.wakey.web.CursorPage;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Maps the caller's session to the tokens module and to the HTTP responses. */
@Component
@RequiredArgsConstructor
final class TokensResources {

    private final ApiTokens tokens;

    CreateTokenResponse create(SessionUser session, String slug, CreateTokenRequest request) {
        TokenOptions options = new TokenOptions(request.scope(), request.expiresAt());
        TokenOwner owner = owner(session);
        CreatedToken created = tokens.create(owner, slug, request.name(), options);
        return CreateTokenResponse.from(created);
    }

    CursorPage<TokenSummary> list(SessionUser session, String slug) {
        var tokensInSpace = tokens.list(owner(session), slug);
        List<TokenSummary> summaries =
                tokensInSpace.stream().map(TokenSummary::from).toList();
        return new CursorPage<>(summaries, null);
    }

    void revoke(SessionUser session, String slug, UUID id) {
        tokens.revoke(owner(session), slug, id);
    }

    private static TokenOwner owner(SessionUser session) {
        var user = session.user();
        return new TokenOwner(user.issuer(), user.subject());
    }
}
