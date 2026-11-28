package dev.wakey.spaces;

import dev.wakey.web.DisplayText;
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
        String normalized = DisplayText.normalize(value);
        return DisplayText.isValid(normalized, max, false);
    }
}
