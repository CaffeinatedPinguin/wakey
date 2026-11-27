package dev.wakey.web;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/** A domain error that the HTTP layer renders as an RFC 9457 error response. */
@Getter
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    private final List<ApiFieldError> fieldErrors;

    private final Map<String, Object> params;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, List.of(), Map.of(), null);
    }

    public ApiException(ErrorCode errorCode, Throwable cause) {
        this(errorCode, List.of(), Map.of(), cause);
    }

    public ApiException(ErrorCode errorCode, Map<String, Object> params) {
        this(errorCode, List.of(), params, null);
    }

    public ApiException(ErrorCode errorCode, List<ApiFieldError> fieldErrors) {
        this(errorCode, fieldErrors, Map.of(), null);
    }

    public ApiException(
            ErrorCode errorCode,
            List<ApiFieldError> fieldErrors,
            Map<String, Object> params,
            @Nullable Throwable cause) {
        super(errorCode.getCode(), cause);
        this.errorCode = errorCode;
        this.fieldErrors = List.copyOf(fieldErrors);
        this.params = Map.copyOf(params);
    }

    /** An error that points at one field with the error's own code. */
    public static ApiException withField(ErrorCode errorCode, String pointer) {
        return fieldError(errorCode, errorCode, pointer, null);
    }

    /** A validation error that points at one field with the field's own code. */
    public static ApiException invalidField(ErrorCode fieldCode, String pointer) {
        return invalidField(fieldCode, pointer, null);
    }

    /** A validation error that points at one field with the field's own code and an underlying cause. */
    public static ApiException invalidField(ErrorCode fieldCode, String pointer, @Nullable Throwable cause) {
        return fieldError(CommonError.VALIDATION_FAILED, fieldCode, pointer, cause);
    }

    private static ApiException fieldError(
            ErrorCode errorCode, ErrorCode fieldCode, String pointer, @Nullable Throwable cause) {
        ApiFieldError fieldError = new ApiFieldError(pointer, fieldCode.getCode(), Map.of());
        return new ApiException(errorCode, List.of(fieldError), Map.of(), cause);
    }
}
