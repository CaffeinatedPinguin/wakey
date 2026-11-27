package dev.wakey.web;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

/** Turns Bean Validation failures into the {@code errors[]} entries of an error body. */
final class ApiFieldErrors {

    private static final String DEFAULT_CODE = CommonError.BAD_REQUEST.getCode();

    private static final List<String> NON_PARAMS = List.of("message", "groups", "payload");

    private ApiFieldErrors() {}

    static List<ApiFieldError> of(MethodArgumentNotValidException exception) {
        List<ApiFieldError> errors = new ArrayList<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.add(fieldError(fieldError));
        }
        return errors;
    }

    static List<ApiFieldError> of(ConstraintViolationException exception) {
        Set<ConstraintViolation<?>> violations = exception.getConstraintViolations();
        List<ApiFieldError> errors = new ArrayList<>(violations.size());
        for (ConstraintViolation<?> violation : violations) {
            errors.add(fieldError(violation));
        }
        return errors;
    }

    private static ApiFieldError fieldError(FieldError error) {
        @Nullable String code = error.getCode();
        String fieldCode = code == null ? DEFAULT_CODE : code;
        String pointer = pointerOf(error.getField());
        Map<String, Object> params = error.contains(ConstraintViolation.class) ? paramsOf(error) : Map.of();
        return new ApiFieldError(pointer, snakeCase(fieldCode), params);
    }

    private static ApiFieldError fieldError(ConstraintViolation<?> violation) {
        String annotation = violation
                .getConstraintDescriptor()
                .getAnnotation()
                .annotationType()
                .getSimpleName();
        String pointer = pointerOf(violation.getPropertyPath());
        return new ApiFieldError(pointer, snakeCase(annotation), paramsOf(violation));
    }

    private static Map<String, Object> paramsOf(FieldError error) {
        @Nullable ConstraintViolation<?> violation = error.unwrap(ConstraintViolation.class);
        if (violation == null) {
            return Map.of();
        }
        return paramsOf(violation);
    }

    private static Map<String, Object> paramsOf(ConstraintViolation<?> violation) {
        Map<String, Object> attributes = violation.getConstraintDescriptor().getAttributes();
        Map<String, Object> filled = new LinkedHashMap<>(attributes);
        filled.keySet().removeAll(NON_PARAMS);
        return filled;
    }

    private static String pointerOf(FieldError error) {
        return pointerOf(error.getField());
    }

    private static String pointerOf(String field) {
        if (field.isEmpty()) {
            return "";
        }
        return "/" + field.replace('.', '/');
    }

    private static String pointerOf(Path path) {
        String name = "";
        for (Path.Node node : path) {
            name = node.getName();
        }
        return pointerOf(name);
    }

    private static String snakeCase(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}
