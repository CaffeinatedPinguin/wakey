package dev.wakey.web;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.OptimisticLockingFailureException;
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

/** The single place that renders every HTTP error as an RFC 9457 error body. */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            @Nullable Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail detail = body instanceof ProblemDetail existing ? existing : ProblemDetail.forStatus(status);
        CommonError error = CommonError.forStatus(status);
        ApiExceptionBodies.apply(detail, error);
        return super.createResponseEntity(detail, headers, status, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail detail = ApiExceptionBodies.of(CommonError.VALIDATION_FAILED, ApiFieldErrors.of(exception));
        return customResponse(detail, headers, status);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return customResponse(ApiExceptionBodies.of(CommonError.INVALID_BODY), headers, status);
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> apiException(ApiException exception) {
        ErrorCode error = exception.getErrorCode();
        ProblemDetail detail = ApiExceptionBodies.of(error, exception.getFieldErrors());
        return errorResponse(error.getStatus(), detail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> constraintViolation(ConstraintViolationException exception) {
        ProblemDetail detail = ApiExceptionBodies.of(CommonError.VALIDATION_FAILED, ApiFieldErrors.of(exception));
        return errorResponse(CommonError.VALIDATION_FAILED.getStatus(), detail);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail forbidden() {
        return ApiExceptionBodies.of(CommonError.FORBIDDEN);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> versionConflict() {
        ErrorCode error = CommonError.VERSION_CONFLICT;
        return errorResponse(error.getStatus(), ApiExceptionBodies.of(error));
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail unauthenticated() {
        return ApiExceptionBodies.of(CommonError.UNAUTHENTICATED);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception exception) {
        log.error("unexpected request failure", exception);
        return errorResponse(CommonError.INTERNAL_ERROR.getStatus(), ApiExceptionBodies.of(CommonError.INTERNAL_ERROR));
    }

    private static ResponseEntity<Object> customResponse(
            ProblemDetail detail, HttpHeaders headers, HttpStatusCode status) {
        return ResponseEntity.status(status)
                .headers(headers)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(detail);
    }

    private static ResponseEntity<ProblemDetail> errorResponse(HttpStatus status, ProblemDetail detail) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(detail);
    }
}
