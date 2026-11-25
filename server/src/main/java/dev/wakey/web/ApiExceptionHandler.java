package dev.wakey.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validationFailed(MethodArgumentNotValidException exception) {
        List<Map<String, Object>> errors = new ArrayList<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.add(fieldError(fieldError));
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
        ProblemDetail problem = Problems.of(exception.getStatus(), exception.getCode(), detailFor(exception));
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

    private static Map<String, Object> fieldError(FieldError error) {
        String code = Objects.requireNonNullElse(error.getCode(), "invalid");
        return Problems.error(pointerOf(error), snakeCase(code));
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

    private static String detailFor(ApiProblemException exception) {
        return switch (exception.getCode()) {
            case NOT_FOUND -> NOT_FOUND_DETAIL;
            case "forbidden" -> "Access is denied.";
            case "slug_taken" -> "A space with that slug already exists.";
            case "validation_failed" -> "The request contains invalid fields.";
            case "conflict" -> "The resource conflicts with the current state.";
            default -> "The request could not be completed.";
        };
    }
}
