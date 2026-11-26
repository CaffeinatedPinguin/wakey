package dev.wakey.spaces.web;

import dev.wakey.spaces.SpaceName;
import dev.wakey.spaces.SpaceSlug;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** The desired state of a new space. */
@Schema(description = "The desired state of a new space")
public record CreateSpaceRequest(
        @Schema(
                description = "The stable space slug",
                example = "acme",
                minLength = SpaceSlug.MIN_LENGTH,
                maxLength = SpaceSlug.MAX_LENGTH,
                pattern = SpaceSlug.PATTERN,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        String slug,

        @Schema(
                description = "The display name, 1-100 characters after trimming",
                example = "Acme",
                maxLength = SpaceName.MAX_LENGTH)
        @NotNull
        String name) {}
