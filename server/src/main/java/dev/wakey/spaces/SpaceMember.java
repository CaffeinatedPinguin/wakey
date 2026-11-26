package dev.wakey.spaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

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

    /** Says whether a value can be an issuer of a membership; the cursor decoder uses the same rule. */
    public static boolean isValidIssuer(@Nullable String value) {
        return isValidIdentity(value, ISSUER_MAX_LENGTH);
    }

    /** Says whether a value can be a subject of a membership; the cursor decoder uses the same rule. */
    public static boolean isValidSubject(@Nullable String value) {
        return isValidIdentity(value, SUBJECT_MAX_LENGTH);
    }

    private static boolean isValidIdentity(@Nullable String value, int maxLength) {
        return value != null
                && !value.isBlank()
                && value.length() <= maxLength
                && !SpaceText.hasForbiddenCodePoint(value);
    }
}
