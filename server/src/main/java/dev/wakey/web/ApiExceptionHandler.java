package dev.wakey.web;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** The single place that renders every HTTP error as RFC 9457 Problem Details. */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final URI PROBLEM_TYPE = URI.create("about:blank");

    private static final String NOT_FOUND = "not_found";

    private static final String NOT_FOUND_DETAIL = "The requested resource was not found.";

    private static final String INTERNAL_ERROR_DETAIL = "An unexpected error occurred.";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validationFailed(MethodArgumentNotValidException exception) {
        List<Map<String, Object>> errors = new ArrayList<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.add(fieldError(fieldError));
        }
        ProblemDetail problem =
                problem(HttpStatus.BAD_REQUEST, "validation_failed", "The request contains invalid fields.");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadableBody() {
        return problem(HttpStatus.BAD_REQUEST, "invalid_body", "The request body could not be read.");
    }

    @ExceptionHandler(ApiProblemException.class)
    ResponseEntity<ProblemDetail> apiProblem(ApiProblemException exception) {
        ProblemDetail problem = problem(exception.getStatus(), exception.getCode(), detailFor(exception));
        if (!exception.getParams().isEmpty()) {
            problem.setProperty("params", exception.getParams());
        }
        return ResponseEntity.status(exception.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(RateLimitedException.class)
    ResponseEntity<ProblemDetail> rateLimited(RateLimitedException exception) {
        long retryAfterSeconds = exception.retryAfter().toSeconds();
        long seconds = Math.max(1L, retryAfterSeconds);
        ProblemDetail problem = problem(HttpStatus.TOO_MANY_REQUESTS, "rate_limited", "Too many requests.");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(seconds))
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail noResource() {
        return problem(HttpStatus.NOT_FOUND, NOT_FOUND, NOT_FOUND_DETAIL);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception exception) {
        log.error("unexpected request failure", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", INTERNAL_ERROR_DETAIL);
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(PROBLEM_TYPE);
        problem.setProperty("code", code);
        problem.setProperty("errors", List.of());
        return problem;
    }

    private static Map<String, Object> fieldError(FieldError error) {
        String code = Objects.requireNonNullElse(error.getCode(), "invalid");
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("pointer", pointerOf(error));
        entry.put("code", snakeCase(code));
        entry.put("params", Map.of());
        return entry;
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

    private static String detailFor(ApiProblemException exception) {
        return switch (exception.getCode()) {
            case NOT_FOUND -> NOT_FOUND_DETAIL;
            case "conflict" -> "The resource conflicts with the current state.";
            default -> "The request could not be completed.";
        };
    }
}
