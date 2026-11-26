package dev.wakey.web;

import java.util.List;
import java.util.Map;
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

    /** An error that points at one field with the error's own code. */
    public static ApiException withField(ErrorCode errorCode, String pointer) {
        FieldProblem problem = new FieldProblem(pointer, errorCode.getCode(), Map.of());
        return new ApiException(errorCode, List.of(problem));
    }

    private ApiException(ErrorCode errorCode, List<FieldProblem> fieldProblems, @Nullable Throwable cause) {
        super(errorCode.getCode(), cause);
        this.errorCode = errorCode;
        this.fieldProblems = List.copyOf(fieldProblems);
    }
}
