package dev.wakey.spaces;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declarative check of the one slug rule; the wire code is {@code invalid_slug}. */
@Documented
@Constraint(validatedBy = InvalidSlugValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface InvalidSlug {

    String message() default "invalid space slug";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int min() default SpaceSlug.MIN_LENGTH;

    int max() default SpaceSlug.MAX_LENGTH;
}
