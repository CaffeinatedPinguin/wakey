package dev.wakey.web;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** The single place that renders every HTTP error as RFC 9457 Problem Details. */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            @Nullable Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = body instanceof ProblemDetail detail ? detail : ProblemDetail.forStatus(status);
        CommonError error = CommonError.forStatus(status);
        ProblemDetails.apply(problem, error);
        return super.createResponseEntity(problem, headers, status, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetails.of(CommonError.VALIDATION_FAILED, FieldProblems.of(exception));
        return customResponse(problem, headers, status);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return customResponse(ProblemDetails.of(CommonError.INVALID_BODY), headers, status);
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> apiException(ApiException exception) {
        ErrorCode error = exception.getErrorCode();
        ProblemDetail problem = ProblemDetails.of(error, exception.getFieldProblems());
        return problemResponse(error.getStatus(), problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> constraintViolation(ConstraintViolationException exception) {
        ProblemDetail problem = ProblemDetails.of(CommonError.VALIDATION_FAILED, FieldProblems.of(exception));
        return problemResponse(CommonError.VALIDATION_FAILED.getStatus(), problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail forbidden() {
        return ProblemDetails.of(CommonError.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail unauthenticated() {
        return ProblemDetails.of(CommonError.UNAUTHENTICATED);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception exception) {
        log.error("unexpected request failure", exception);
        return problemResponse(CommonError.INTERNAL_ERROR.getStatus(), ProblemDetails.of(CommonError.INTERNAL_ERROR));
    }

    private static ResponseEntity<Object> customResponse(
            ProblemDetail problem, HttpHeaders headers, HttpStatusCode status) {
        return ResponseEntity.status(status)
                .headers(headers)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private static ResponseEntity<ProblemDetail> problemResponse(HttpStatus status, ProblemDetail problem) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
