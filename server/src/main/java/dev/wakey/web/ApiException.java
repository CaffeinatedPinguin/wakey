package dev.wakey.web;

import java.util.List;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/** A domain error that the HTTP layer renders as an RFC 9457 Problem Details response. */
@Getter
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    private final List<FieldProblem> fieldProblems;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, List.of());
    }

    public ApiException(ErrorCode errorCode, Throwable cause) {
        this(errorCode, List.of(), cause);
    }

    public ApiException(ErrorCode errorCode, List<FieldProblem> fieldProblems) {
        this(errorCode, fieldProblems, null);
    }

    private ApiException(ErrorCode errorCode, List<FieldProblem> fieldProblems, @Nullable Throwable cause) {
        super(errorCode.getCode(), cause);
        this.errorCode = errorCode;
        this.fieldProblems = List.copyOf(fieldProblems);
    }
}
