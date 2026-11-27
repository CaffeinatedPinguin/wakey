package dev.wakey.spaces.web;

import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceSlug;
import dev.wakey.spaces.SpaceView;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/** A space as seen by the caller, with the caller's membership role. */
@Schema(description = "A space as seen by the caller")
public record SpaceResponse(
        @Schema(
                description = "The stable space slug",
                example = "acme",
                pattern = SpaceSlug.PATTERN,
                requiredMode = Schema.RequiredMode.REQUIRED)
        String slug,

        @Schema(description = "The display name", example = "Acme", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(
                description = "The version growing whenever the name changes",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        long version,

        @Schema(
                description = "The caller's membership role, or null for an Admin who is not a member",
                requiredMode = Schema.RequiredMode.REQUIRED,
                implementation = SpaceRole.class,
                enumAsRef = true)
        @Nullable
        SpaceRole role) {

    static SpaceResponse from(SpaceView view) {
        return new SpaceResponse(view.slug(), view.name(), view.version(), view.role());
    }
}
