package dev.wakey.api.spaces;

import dev.wakey.spaces.SpaceName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** The desired state of a space; the name is trimmed and must stay within the domain limit. */
@Schema(description = "The desired state of a space")
public record PutSpaceRequest(
        @Schema(
                description = "The display name, 1-100 characters after trimming",
                example = "Acme",
                minLength = 1,
                maxLength = SpaceName.MAX_LENGTH)
        @NotNull
        String name) {}
