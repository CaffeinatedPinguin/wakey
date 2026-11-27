package dev.wakey.tokens.web;

import dev.wakey.tokens.CreatedToken;
import io.swagger.v3.oas.annotations.media.Schema;

/** The one response that carries the token secret; every later response carries only the summary. */
@Schema(description = "A newly created API token and its secret, shown only once")
public record CreateTokenResponse(
        @Schema(
                description = "The token secret, shown only in this response",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String secret,

        @Schema(
                description = "The stored token, without the secret or the hash",
                requiredMode = Schema.RequiredMode.REQUIRED)
        TokenSummary token) {

    static CreateTokenResponse from(CreatedToken created) {
        TokenSummary token = TokenSummary.from(created.token());
        return new CreateTokenResponse(created.secret(), token);
    }

    @Override
    public String toString() {
        return "CreateTokenResponse[token=" + token + ", secret=<redacted>]";
    }
}
