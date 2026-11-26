package dev.wakey.spaces;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.Nullable;

/** Validates a member subject against the shared {@link SpaceMember} identity rule. */
public final class InvalidSubjectValidator implements ConstraintValidator<InvalidSubject, String> {

    private int max;

    @Override
    public void initialize(InvalidSubject annotation) {
        this.max = annotation.max();
    }

    @Override
    public boolean isValid(@Nullable String value, ConstraintValidatorContext context) {
        return SpaceMember.isValidIdentity(value, max);
    }
}
