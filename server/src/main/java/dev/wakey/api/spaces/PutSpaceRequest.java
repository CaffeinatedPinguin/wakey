package dev.wakey.api.spaces;

import dev.wakey.spaces.SpaceName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.jspecify.annotations.Nullable;

/** The desired state of a space; the name is trimmed and must stay within the domain limit. */
@Schema(description = "The desired state of a space")
public record PutSpaceRequest(
        @Schema(
                description = "The display name, 1-100 characters after trimming",
                example = "Acme",
                maxLength = SpaceName.MAX_LENGTH)
        @NotNull
        String name,

        @Schema(
                description = "The version the caller read; omit to rename without a condition",
                example = "1",
                minimum = "0")
        @Nullable
        @PositiveOrZero
        Long version) {}
