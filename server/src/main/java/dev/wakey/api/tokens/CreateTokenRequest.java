package dev.wakey.api.tokens;

import dev.wakey.tokens.TokenScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/** A token to create; the scope is the greatest scope the owner may use, capped again by the role. */
@Schema(description = "A new API token")
public record CreateTokenRequest(
        @Schema(description = "The token name", example = "ci-deploy", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 100)
        String name,

        @Schema(description = "The greatest scope the token may use", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        TokenScope scope,

        @Schema(
                description = "When the token expires, or null for no expiry",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                nullable = true)
        @Nullable
        Instant expiresAt) {}
