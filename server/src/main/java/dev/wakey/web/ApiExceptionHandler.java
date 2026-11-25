package dev.wakey.web;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** The single place that renders every HTTP error as RFC 9457 Problem Details. */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String NOT_FOUND = "not_found";

    private static final String BAD_REQUEST = "bad_request";

    private static final String NOT_FOUND_DETAIL = "The requested resource was not found.";

    private static final String BAD_REQUEST_DETAIL = "The request is invalid.";

    private static final String INTERNAL_ERROR_DETAIL = "An unexpected error occurred.";

    private static final Set<String> CONSTRAINT_PARAMS = Set.of("min", "max", "value");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validationFailed(MethodArgumentNotValidException exception) {
        List<Map<String, Object>> errors = new ArrayList<>();
        Object target = exception.getBindingResult().getTarget();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.add(fieldError(fieldError, target));
        }
        ProblemDetail problem =
                Problems.of(HttpStatus.BAD_REQUEST, "validation_failed", "The request contains invalid fields.");
        return Problems.withErrors(problem, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadableBody() {
        return Problems.of(HttpStatus.BAD_REQUEST, "invalid_body", "The request body could not be read.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail typeMismatch() {
        return Problems.of(HttpStatus.BAD_REQUEST, BAD_REQUEST, BAD_REQUEST_DETAIL);
    }

    @ExceptionHandler(ApiProblemException.class)
    ResponseEntity<ProblemDetail> apiProblem(ApiProblemException exception) {
        ProblemDetail problem = Problems.of(exception.getStatus(), exception.getCode(), exception.getDetail());
        if (!exception.getErrors().isEmpty()) {
            Problems.withErrors(problem, exception.getErrors());
        }
        if (!exception.getParams().isEmpty()) {
            problem.setProperty("params", exception.getParams());
        }
        return problemResponse(exception.getStatus(), problem);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail noResource() {
        return Problems.of(HttpStatus.NOT_FOUND, NOT_FOUND, NOT_FOUND_DETAIL);
    }

    @ExceptionHandler(AccessDeniedException.class)
    void accessDenied(AccessDeniedException exception) {
        throw exception;
    }

    @ExceptionHandler(AuthenticationException.class)
    void authentication(AuthenticationException exception) {
        throw exception;
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception exception) {
        if (exception instanceof ErrorResponse errorResponse) {
            return springClientError(errorResponse);
        }
        log.error("unexpected request failure", exception);
        ProblemDetail problem = Problems.of(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", INTERNAL_ERROR_DETAIL);
        return problemResponse(HttpStatus.INTERNAL_SERVER_ERROR, problem);
    }

    private static ResponseEntity<ProblemDetail> springClientError(ErrorResponse exception) {
        int statusCode = exception.getStatusCode().value();
        HttpStatus status = HttpStatus.valueOf(statusCode);
        ProblemDetail problem = Problems.of(status, codeFor(status), detailForStatus(status));
        return ResponseEntity.status(status)
                .headers(exception.getHeaders())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private static ResponseEntity<ProblemDetail> problemResponse(HttpStatus status, ProblemDetail problem) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private static Map<String, Object> fieldError(FieldError error, @Nullable Object target) {
        String code = Objects.requireNonNullElse(error.getCode(), "invalid");
        return Problems.error(pointerOf(error), snakeCase(code), paramsOf(error, target));
    }

    private static Map<String, Object> paramsOf(FieldError error, @Nullable Object target) {
        Annotation annotation = constraintAnnotation(error, target);
        if (annotation == null) {
            return Map.of();
        }
        Map<String, Object> params = new LinkedHashMap<>();
        for (Method attribute : annotation.annotationType().getDeclaredMethods()) {
            if (CONSTRAINT_PARAMS.contains(attribute.getName())) {
                putAttribute(params, attribute, annotation);
            }
        }
        return params;
    }

    private static void putAttribute(Map<String, Object> params, Method attribute, Annotation annotation) {
        try {
            params.put(attribute.getName(), attribute.invoke(annotation));
        } catch (ReflectiveOperationException exception) {
            log.debug("could not read constraint attribute {}", attribute.getName(), exception);
        }
    }

    private static @Nullable Annotation constraintAnnotation(FieldError error, @Nullable Object target) {
        Annotation found = null;
        String code = error.getCode();
        if (target != null && code != null) {
            try {
                Class<?> targetClass = target.getClass();
                String fieldName = attributeName(error.getField());
                Field field = targetClass.getDeclaredField(fieldName);
                found = matchingAnnotation(field, code);
            } catch (NoSuchFieldException exception) {
                log.debug("no field for validation error on {}", error.getField(), exception);
            }
        }
        return found;
    }

    private static @Nullable Annotation matchingAnnotation(Field field, String code) {
        Annotation match = null;
        for (Annotation annotation : field.getAnnotations()) {
            String annotationName = annotation.annotationType().getSimpleName();
            if (annotationName.equals(code)) {
                match = annotation;
            }
        }
        return match;
    }

    private static String attributeName(String fieldPath) {
        int end = fieldPath.length();
        int dot = fieldPath.indexOf('.');
        int bracket = fieldPath.indexOf('[');
        if (dot >= 0) {
            end = Math.min(end, dot);
        }
        if (bracket >= 0) {
            end = Math.min(end, bracket);
        }
        return fieldPath.substring(0, end);
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

    private static String codeFor(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> BAD_REQUEST;
            case NOT_FOUND -> NOT_FOUND;
            case METHOD_NOT_ALLOWED -> "method_not_allowed";
            case NOT_ACCEPTABLE -> "not_acceptable";
            case UNSUPPORTED_MEDIA_TYPE -> "unsupported_media_type";
            case INTERNAL_SERVER_ERROR -> "internal_error";
            default -> status.is4xxClientError() ? BAD_REQUEST : "internal_error";
        };
    }

    private static String detailForStatus(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> BAD_REQUEST_DETAIL;
            case NOT_FOUND -> NOT_FOUND_DETAIL;
            case METHOD_NOT_ALLOWED -> "The HTTP method is not allowed for this resource.";
            case NOT_ACCEPTABLE -> "The requested media type cannot be produced.";
            case UNSUPPORTED_MEDIA_TYPE -> "The request media type is not supported.";
            default -> status.is5xxServerError() ? INTERNAL_ERROR_DETAIL : BAD_REQUEST_DETAIL;
        };
    }
}
