package dev.wakey.spaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A membership identity and role: the OIDC issuer and subject pair plus the role in the space. The
 * length limits are the schema invariants and are checked at the service boundary.
 */
public record SpaceMember(
        @NotBlank @Size(max = SpaceMember.ISSUER_MAX_LENGTH) @InvalidSubject
        String issuer,

        @NotBlank @Size(max = SpaceMember.SUBJECT_MAX_LENGTH) @InvalidSubject
        String subject,

        @NotNull SpaceRole role) {

    public static final int ISSUER_MAX_LENGTH = 255;

    public static final int SUBJECT_MAX_LENGTH = 255;
}
