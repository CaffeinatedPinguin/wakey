package dev.wakey.spaces;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.Nullable;

/** Validates a member issuer against the shared {@link SpaceMember} identity rule. */
public final class InvalidIssuerValidator implements ConstraintValidator<InvalidIssuer, String> {

    private int max;

    @Override
    public void initialize(InvalidIssuer annotation) {
        this.max = annotation.max();
    }

    @Override
    public boolean isValid(@Nullable String value, ConstraintValidatorContext context) {
        return SpaceMember.isValidIdentity(value, max);
    }
}
