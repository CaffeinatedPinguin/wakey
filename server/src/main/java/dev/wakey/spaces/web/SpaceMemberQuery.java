package dev.wakey.spaces.web;

import dev.wakey.spaces.InvalidIssuer;
import dev.wakey.spaces.InvalidSubject;
import dev.wakey.spaces.SpaceMember;
import dev.wakey.spaces.SpaceMemberId;
import io.swagger.v3.oas.annotations.media.Schema;

/** The issuer and subject that identify a member in a query. */
public record SpaceMemberQuery(
        @Schema(
                description = "The OIDC issuer of the member",
                maxLength = SpaceMember.ISSUER_MAX_LENGTH,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @InvalidIssuer
        String issuer,

        @Schema(
                description = "The OIDC subject of the member",
                maxLength = SpaceMember.SUBJECT_MAX_LENGTH,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @InvalidSubject
        String subject) {

    SpaceMemberId toId() {
        return new SpaceMemberId(issuer, subject);
    }
}
