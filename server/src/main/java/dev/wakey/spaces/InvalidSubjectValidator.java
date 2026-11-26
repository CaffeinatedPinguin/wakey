package dev.wakey.spaces;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.Nullable;

/** Validates a member identity against the shared {@link SpaceText} character rule. */
public final class InvalidSubjectValidator implements ConstraintValidator<InvalidSubject, String> {

    @Override
    public boolean isValid(@Nullable String value, ConstraintValidatorContext context) {
        return value == null || !SpaceText.hasForbiddenCodePoint(value);
    }
}
