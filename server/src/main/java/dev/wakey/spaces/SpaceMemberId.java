package dev.wakey.spaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The OIDC issuer and subject pair that identifies a member, without the role. */
public record SpaceMemberId(
        @NotBlank @Size(max = SpaceMember.ISSUER_MAX_LENGTH) @InvalidSubject
        String issuer,

        @NotBlank @Size(max = SpaceMember.SUBJECT_MAX_LENGTH) @InvalidSubject
        String subject) {}
