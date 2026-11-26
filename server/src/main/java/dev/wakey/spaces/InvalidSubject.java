package dev.wakey.spaces;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Checks a member subject against the one identity rule: not blank, at most {@link #max()} characters
 * and no control, format, line or paragraph separator characters. The wire code is
 * {@code invalid_subject}.
 */
@Documented
@Constraint(validatedBy = InvalidSubjectValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface InvalidSubject {

    String message() default "invalid member identity";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int max() default SpaceMember.SUBJECT_MAX_LENGTH;
}
