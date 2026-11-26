package dev.wakey.spaces.web;

import dev.wakey.spaces.SpaceMember;
import dev.wakey.spaces.SpaceMemberId;
import dev.wakey.spaces.SpaceRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** The new role of one member. */
@Schema(description = "The new role of a member")
public record ChangeSpaceMemberRoleRequest(
        @Schema(
                description = "The role in the space",
                requiredMode = Schema.RequiredMode.REQUIRED,
                implementation = SpaceRole.class,
                enumAsRef = true)
        @NotNull
        SpaceRole role) {

    SpaceMember toMember(SpaceMemberId member) {
        return new SpaceMember(member.issuer(), member.subject(), role);
    }
}
