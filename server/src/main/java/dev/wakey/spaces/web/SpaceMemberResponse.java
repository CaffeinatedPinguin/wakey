package dev.wakey.spaces.web;

import dev.wakey.spaces.SpaceMember;
import dev.wakey.spaces.SpaceRole;
import io.swagger.v3.oas.annotations.media.Schema;

/** One membership of a space. */
@Schema(description = "A membership of a space")
public record SpaceMemberResponse(
        @Schema(
                description = "The OIDC issuer of the member",
                example = "https://issuer.example.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String issuer,

        @Schema(
                description = "The OIDC subject of the member",
                example = "user-42",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String subject,

        @Schema(
                description = "The role in the space",
                requiredMode = Schema.RequiredMode.REQUIRED,
                implementation = SpaceRole.class,
                enumAsRef = true)
        SpaceRole role) {

    static SpaceMemberResponse from(SpaceMember member) {
        return new SpaceMemberResponse(member.issuer(), member.subject(), member.role());
    }
}
