package dev.wakey.web;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/** The error codes every module shares; unknown statuses still map to a code and a detail. */
@Getter
@RequiredArgsConstructor
public enum CommonError implements ErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "The request is invalid."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "The request contains invalid fields."),
    INVALID_BODY(HttpStatus.BAD_REQUEST, "The request body could not be read."),
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "The cursor is invalid."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication is required."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access is denied."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "The requested resource was not found."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "The HTTP method is not allowed for this resource."),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "The requested media type cannot be produced."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The request media type is not supported."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");

    private final HttpStatus status;

    private final String detail;

    public static CommonError forStatus(HttpStatusCode status) {
        for (CommonError error : values()) {
            if (error.status.value() == status.value()) {
                return error;
            }
        }
        return status.is4xxClientError() ? BAD_REQUEST : INTERNAL_ERROR;
    }
}
