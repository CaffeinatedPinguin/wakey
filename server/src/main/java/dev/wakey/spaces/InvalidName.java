package dev.wakey.spaces;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declarative check of the one space-name rule; the wire code is {@code invalid_name}. */
@Documented
@Constraint(validatedBy = InvalidNameValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface InvalidName {

    String message() default "invalid space name";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int max() default SpaceName.MAX_LENGTH;
}
