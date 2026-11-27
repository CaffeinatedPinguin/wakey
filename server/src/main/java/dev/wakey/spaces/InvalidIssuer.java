package dev.wakey.spaces;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Checks a member issuer against the one identity rule: not blank, at most {@link #max()} characters
 * and no control, format, line or paragraph separator characters. The wire code is
 * {@code invalid_issuer}.
 */
@Documented
@Constraint(validatedBy = InvalidIssuerValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface InvalidIssuer {

    String message() default "invalid member issuer";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int max() default SpaceMember.ISSUER_MAX_LENGTH;
}
