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

/** Turns Bean Validation failures into the {@code errors[]} entries of a Problem Details body. */
final class FieldProblems {

    private static final String DEFAULT_CODE = CommonError.BAD_REQUEST.getCode();

    private static final List<String> NON_PARAMS = List.of("message", "groups", "payload");

    private FieldProblems() {}

    static List<FieldProblem> of(MethodArgumentNotValidException exception) {
        List<FieldProblem> problems = new ArrayList<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            problems.add(fieldProblem(fieldError));
        }
        return problems;
    }

    static List<FieldProblem> of(ConstraintViolationException exception) {
        Set<ConstraintViolation<?>> violations = exception.getConstraintViolations();
        List<FieldProblem> problems = new ArrayList<>(violations.size());
        for (ConstraintViolation<?> violation : violations) {
            problems.add(fieldProblem(violation));
        }
        return problems;
    }

    private static FieldProblem fieldProblem(FieldError error) {
        @Nullable String code = error.getCode();
        String fieldCode = code == null ? DEFAULT_CODE : code;
        String pointer = pointerOf(error.getField());
        Map<String, Object> params = error.contains(ConstraintViolation.class) ? paramsOf(error) : Map.of();
        return new FieldProblem(pointer, snakeCase(fieldCode), params);
    }

    private static FieldProblem fieldProblem(ConstraintViolation<?> violation) {
        String annotation = violation
                .getConstraintDescriptor()
                .getAnnotation()
                .annotationType()
                .getSimpleName();
        String pointer = pointerOf(violation.getPropertyPath());
        return new FieldProblem(pointer, snakeCase(annotation), paramsOf(violation));
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
