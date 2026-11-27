package dev.wakey.checks.web;

import dev.wakey.checks.HttpUrlRule;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.Nullable;

/** The Bean Validation adapter of the one {@link HttpUrlRule}; it only strips the surrounding spaces. */
public final class HttpUrlValidator implements ConstraintValidator<HttpUrl, String> {

    @Override
    public boolean isValid(@Nullable String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return HttpUrlRule.isHttpUrl(value.strip());
    }
}
