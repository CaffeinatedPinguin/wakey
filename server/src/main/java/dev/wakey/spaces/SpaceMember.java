package dev.wakey.spaces;

import dev.wakey.web.DisplayText;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

/**
 * A membership identity and role: the OIDC issuer and subject pair plus the role in the space. The
 * one identity rule is shared with the declarative constraints of the web layer.
 */
public record SpaceMember(
        @InvalidIssuer String issuer,

        @InvalidSubject String subject,

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

    static boolean isValidIdentity(@Nullable String value, int maxLength) {
        return value != null
                && !value.isBlank()
                && value.length() <= maxLength
                && !DisplayText.hasForbiddenCodePoint(value);
    }
}
