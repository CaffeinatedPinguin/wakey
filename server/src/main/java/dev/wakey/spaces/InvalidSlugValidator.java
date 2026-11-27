package dev.wakey.spaces;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.Nullable;

/** Validates a candidate against the {@link SpaceSlug} rule; a null value is left to the null checks. */
public final class InvalidSlugValidator implements ConstraintValidator<InvalidSlug, String> {

    @Override
    public boolean isValid(@Nullable String value, ConstraintValidatorContext context) {
        return value == null || SpaceSlug.isValid(value);
    }
}
