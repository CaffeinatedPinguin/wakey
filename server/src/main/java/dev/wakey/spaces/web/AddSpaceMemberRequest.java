package dev.wakey.spaces.web;

import dev.wakey.spaces.InvalidIssuer;
import dev.wakey.spaces.InvalidSubject;
import dev.wakey.spaces.SpaceMember;
import dev.wakey.spaces.SpaceRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** A membership to add; the limits are the same ones the domain record enforces. */
@Schema(description = "A membership to add")
public record AddSpaceMemberRequest(
        @Schema(
                description = "The OIDC issuer of the member",
                example = "https://issuer.example.com",
                maxLength = SpaceMember.ISSUER_MAX_LENGTH,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @InvalidIssuer
        String issuer,

        @Schema(
                description = "The OIDC subject of the member",
                example = "user-42",
                maxLength = SpaceMember.SUBJECT_MAX_LENGTH,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @InvalidSubject
        String subject,

        @Schema(
                description = "The role in the space",
                requiredMode = Schema.RequiredMode.REQUIRED,
                implementation = SpaceRole.class,
                enumAsRef = true)
        @NotNull
        SpaceRole role) {

    SpaceMember toMember() {
        return new SpaceMember(issuer, subject, role);
    }
}
