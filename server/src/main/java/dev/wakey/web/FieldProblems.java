package dev.wakey.web;

import jakarta.validation.ConstraintViolation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    private static FieldProblem fieldProblem(FieldError error) {
        @Nullable String code = error.getCode();
        String fieldCode = code == null ? DEFAULT_CODE : code;
        return new FieldProblem(pointerOf(error), snakeCase(fieldCode), paramsOf(error));
    }

    private static Map<String, Object> paramsOf(FieldError error) {
        if (!error.contains(ConstraintViolation.class)) {
            return Map.of();
        }
        ConstraintViolation<?> violation = error.unwrap(ConstraintViolation.class);
        Map<String, Object> params = Map.of();
        if (violation != null) {
            Map<String, Object> attributes = violation.getConstraintDescriptor().getAttributes();
            Map<String, Object> filled = new LinkedHashMap<>(attributes);
            filled.keySet().removeAll(NON_PARAMS);
            params = filled;
        }
        return params;
    }

    private static String pointerOf(FieldError error) {
        String field = error.getField();
        if (field.isEmpty()) {
            return "";
        }
        return "/" + field.replace('.', '/');
    }

    private static String snakeCase(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}
