package dev.wakey.api.spaces;

import dev.wakey.spaces.InvalidSpaceException;
import dev.wakey.spaces.SpaceAccessDeniedException;
import dev.wakey.spaces.SpaceNotFoundException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Renders the spaces domain errors as RFC 9457 Problem Details for the spaces endpoints only. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = SpacesController.class)
final class SpacesExceptionHandler {

    private static final URI PROBLEM_TYPE = URI.create("about:blank");

    private static final String VALIDATION_FAILED = "validation_failed";

    @ExceptionHandler(SpaceNotFoundException.class)
    ProblemDetail notFound() {
        return problem(HttpStatus.NOT_FOUND, "not_found", "The requested space was not found.");
    }

    @ExceptionHandler(SpaceAccessDeniedException.class)
    ProblemDetail forbidden() {
        return problem(HttpStatus.FORBIDDEN, "forbidden", "Access is denied.");
    }

    @ExceptionHandler(InvalidSpaceException.class)
    ProblemDetail invalid(InvalidSpaceException exception) {
        ProblemDetail problem =
                problem(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, "The request contains invalid fields.");
        Map<String, Object> error = fieldError(exception);
        problem.setProperty("errors", List.of(error));
        return problem;
    }

    private static Map<String, Object> fieldError(InvalidSpaceException exception) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("pointer", "/" + exception.field());
        entry.put("code", exception.code());
        entry.put("params", Map.of());
        return entry;
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(PROBLEM_TYPE);
        problem.setProperty("code", code);
        problem.setProperty("errors", List.of());
        return problem;
    }
}
