package dev.wakey.tokens.web;

import dev.wakey.spaces.web.SessionUser;
import dev.wakey.spaces.web.SpaceReader;
import dev.wakey.web.CursorPage;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * The HTTP adapter for API tokens; all access rules and validation live in the tokens module.
 * These endpoints accept a session only, so a token cannot mint or revoke tokens.
 */
@RestController
@Tag(name = "tokens")
@RequiredArgsConstructor
final class TokensController {

    private final TokensResources resources;

    @CreateTokenOperation
    ResponseEntity<CreateTokenResponse> createToken(
            @PathVariable String slug,
            SpaceReader reader,
            SessionUser session,
            @Valid @RequestBody CreateTokenRequest request) {
        CreateTokenResponse response = resources.create(session, slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ListTokensOperation
    CursorPage<TokenSummary> listTokens(@PathVariable String slug, SpaceReader reader, SessionUser session) {
        return resources.list(session, slug);
    }

    @RevokeTokenOperation
    ResponseEntity<Void> revokeToken(
            @PathVariable String slug, @PathVariable UUID id, SpaceReader reader, SessionUser session) {
        resources.revoke(session, slug, id);
        return ResponseEntity.noContent().build();
    }
}
