package dev.wakey.api;

import dev.wakey.checks.CheckNotFoundException;
import dev.wakey.checks.CheckVersionConflictException;
import dev.wakey.checks.InvalidCheckException;
import dev.wakey.settings.InvalidSettingException;
import dev.wakey.settings.SettingOutOfPlanException;
import dev.wakey.settings.UnknownPlanException;
import dev.wakey.spaces.InvalidSpaceException;
import dev.wakey.spaces.SlugTakenException;
import dev.wakey.spaces.SpaceAccessDeniedException;
import dev.wakey.spaces.SpaceNotFoundException;
import dev.wakey.tokens.InvalidTokenException;
import dev.wakey.web.Problems;
import java.util.List;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The single place that maps the domain exceptions of the modules to the Problem Details contract.
 * It runs before the framework-level advice in {@code web}, which does not know the domain.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class DomainExceptionHandler {

    private static final String NOT_FOUND = "not_found";

    private static final String FORBIDDEN = "forbidden";

    private static final String VALIDATION_FAILED = "validation_failed";

    private static final String NOT_FOUND_DETAIL = "The requested resource was not found.";

    private static final String FORBIDDEN_DETAIL = "Access is denied.";

    private static final String VALIDATION_DETAIL = "The request contains invalid fields.";

    private static final String SLUG_TAKEN_DETAIL = "A space with that slug already exists.";

    private static final String DEFAULT_DETAIL = "The request could not be completed.";

    @ExceptionHandler({SpaceNotFoundException.class, CheckNotFoundException.class})
    ResponseEntity<ProblemDetail> notFound() {
        return problemResponse(HttpStatus.NOT_FOUND, NOT_FOUND, NOT_FOUND_DETAIL);
    }

    @ExceptionHandler(SpaceAccessDeniedException.class)
    ResponseEntity<ProblemDetail> forbidden() {
        return problemResponse(HttpStatus.FORBIDDEN, FORBIDDEN, FORBIDDEN_DETAIL);
    }

    @ExceptionHandler(SlugTakenException.class)
    ResponseEntity<ProblemDetail> slugTaken() {
        return problemResponse(HttpStatus.CONFLICT, "slug_taken", SLUG_TAKEN_DETAIL);
    }

    @ExceptionHandler(CheckVersionConflictException.class)
    ResponseEntity<ProblemDetail> versionConflict() {
        return problemResponse(HttpStatus.CONFLICT, "version_conflict", DEFAULT_DETAIL);
    }

    @ExceptionHandler(InvalidCheckException.class)
    ResponseEntity<ProblemDetail> invalidCheck(InvalidCheckException exception) {
        return validationProblem("/" + exception.field(), exception.code());
    }

    @ExceptionHandler(InvalidSpaceException.class)
    ResponseEntity<ProblemDetail> invalidSpace(InvalidSpaceException exception) {
        return validationProblem("/" + exception.field(), exception.code());
    }

    @ExceptionHandler(InvalidTokenException.class)
    ResponseEntity<ProblemDetail> invalidToken(InvalidTokenException exception) {
        return validationProblem("/" + exception.field(), exception.code());
    }

    @ExceptionHandler(InvalidSettingException.class)
    ResponseEntity<ProblemDetail> invalidSetting(InvalidSettingException exception) {
        String pointer = exception.value().isEmpty() ? "/key" : "/value";
        return validationProblem(pointer, "invalid_value");
    }

    @ExceptionHandler(SettingOutOfPlanException.class)
    ResponseEntity<ProblemDetail> outOfPlan(SettingOutOfPlanException exception) {
        Map<String, Object> params =
                Map.of("bound", exception.bound(), "planName", exception.planName(), "key", exception.key());
        return paramsProblem("out_of_plan", params);
    }

    @ExceptionHandler(UnknownPlanException.class)
    ResponseEntity<ProblemDetail> unknownPlan(UnknownPlanException exception) {
        Map<String, Object> params = Map.of("plan", exception.planName());
        return paramsProblem("unknown_plan", params);
    }

    private static ResponseEntity<ProblemDetail> validationProblem(String pointer, String code) {
        Map<String, Object> error = Problems.error(pointer, code);
        List<Map<String, Object>> errors = List.of(error);
        ProblemDetail problem = Problems.of(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, VALIDATION_DETAIL);
        return problemResponse(HttpStatus.BAD_REQUEST, problem, errors, Map.of());
    }

    private static ResponseEntity<ProblemDetail> paramsProblem(String code, Map<String, Object> params) {
        ProblemDetail problem = Problems.of(HttpStatus.BAD_REQUEST, code, DEFAULT_DETAIL);
        return problemResponse(HttpStatus.BAD_REQUEST, problem, List.of(), params);
    }

    private static ResponseEntity<ProblemDetail> problemResponse(HttpStatus status, String code, String detail) {
        ProblemDetail problem = Problems.of(status, code, detail);
        return problemResponse(status, problem, List.of(), Map.of());
    }

    private static ResponseEntity<ProblemDetail> problemResponse(
            HttpStatus status, ProblemDetail problem, List<Map<String, Object>> errors, Map<String, Object> params) {
        if (!errors.isEmpty()) {
            Problems.withErrors(problem, errors);
        }
        if (!params.isEmpty()) {
            problem.setProperty("params", params);
        }
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
