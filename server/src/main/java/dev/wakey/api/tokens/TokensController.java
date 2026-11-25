package dev.wakey.api.tokens;

import dev.wakey.tokens.ApiTokens;
import dev.wakey.tokens.TokenSummary;
import dev.wakey.web.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * The HTTP adapter for API tokens; all access rules and validation live in the tokens module.
 * These endpoints accept a session only, never a token, so a token cannot mint or revoke tokens.
 */
@RestController
final class TokensController {

    private static final String COLLECTION_PATH = "/api/v1/spaces/{slug}/tokens";

    private static final String ITEM_PATH = COLLECTION_PATH + "/{id}";

    private static final String JSON = "application/json";

    private static final String PROBLEM_JSON = "application/problem+json";

    private final ApiTokens tokens;

    TokensController(ApiTokens tokens) {
        this.tokens = tokens;
    }

    @Operation(operationId = "createToken", summary = "Create an API token for a space")
    @ApiResponse(
            responseCode = "201",
            description = "The token was created; the secret is returned only here.",
            content = @Content(mediaType = JSON, schema = @Schema(implementation = CreateTokenResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description = "The token request is invalid.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "The caller is not allowed to create this token.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "The space was not found.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(path = COLLECTION_PATH, consumes = JSON, produces = JSON)
    ResponseEntity<CreateTokenResponse> createToken(
            @PathVariable String slug, @Valid @RequestBody CreateTokenRequest request, Authentication authentication) {
        CreateTokenResponse response = TokenErrors.create(tokens, authentication, slug, request);
        return ResponseEntity.status(201).body(response);
    }

    @Operation(operationId = "listTokens", summary = "List the caller's API tokens in a space")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "The space was not found.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping(path = COLLECTION_PATH, produces = JSON)
    CursorPage<TokenSummary> listTokens(@PathVariable String slug, Authentication authentication) {
        List<TokenSummary> summaries = TokenErrors.list(tokens, authentication, slug);
        return new CursorPage<>(summaries, null);
    }

    @Operation(operationId = "revokeToken", summary = "Revoke one of the caller's API tokens")
    @ApiResponse(responseCode = "204", description = "The token was revoked.")
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "The caller is not allowed to revoke this token.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "The space was not found.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping(path = ITEM_PATH)
    ResponseEntity<Void> revokeToken(@PathVariable String slug, @PathVariable UUID id, Authentication authentication) {
        TokenErrors.revoke(tokens, authentication, slug, id);
        return ResponseEntity.noContent().build();
    }
}
