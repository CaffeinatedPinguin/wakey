package dev.wakey.spaces;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.Nullable;

/** Validates the normalized name against the {@link SpaceName} rule. */
public final class InvalidNameValidator implements ConstraintValidator<InvalidName, String> {

    private int max;

    @Override
    public void initialize(InvalidName annotation) {
        this.max = annotation.max();
    }

    @Override
    public boolean isValid(@Nullable String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String normalized = SpaceName.normalize(value);
        int length = normalized.codePointCount(0, normalized.length());
        return length >= 1 && length <= max && !SpaceText.hasForbiddenCodePoint(normalized);
    }
}
