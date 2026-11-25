package dev.wakey.web;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;

/**
 * A domain error that the HTTP layer renders as an RFC 9457 Problem Details response. Modules throw it
 * without knowing about Spring MVC, and they set the user-facing {@code detail} themselves.
 */
public class ApiProblemException extends RuntimeException {

    private static final String DEFAULT_DETAIL = "The request could not be completed.";

    private final HttpStatus status;

    private final String code;

    private final String detail;

    private final List<Map<String, Object>> errors;

    private final Map<String, Object> params;

    public ApiProblemException(HttpStatus status, String code) {
        super(code);
        this.status = status;
        this.code = code;
        this.detail = DEFAULT_DETAIL;
        this.errors = List.of();
        this.params = Map.of();
    }

    public ApiProblemException(HttpStatus status, String code, @Nullable Throwable cause) {
        super(code, cause);
        this.status = status;
        this.code = code;
        this.detail = DEFAULT_DETAIL;
        this.errors = List.of();
        this.params = Map.of();
    }

    public ApiProblemException(HttpStatus status, String code, Map<String, Object> params) {
        super(code);
        this.status = status;
        this.code = code;
        this.detail = DEFAULT_DETAIL;
        this.errors = List.of();
        this.params = Map.copyOf(params);
    }

    public ApiProblemException(HttpStatus status, String code, Map<String, Object> params, @Nullable Throwable cause) {
        super(code, cause);
        this.status = status;
        this.code = code;
        this.detail = DEFAULT_DETAIL;
        this.errors = List.of();
        this.params = Map.copyOf(params);
    }

    public ApiProblemException(HttpStatus status, String code, List<Map<String, Object>> errors) {
        super(code);
        this.status = status;
        this.code = code;
        this.detail = DEFAULT_DETAIL;
        this.errors = List.copyOf(errors);
        this.params = Map.of();
    }

    public ApiProblemException(
            HttpStatus status, String code, List<Map<String, Object>> errors, @Nullable Throwable cause) {
        super(code, cause);
        this.status = status;
        this.code = code;
        this.detail = DEFAULT_DETAIL;
        this.errors = List.copyOf(errors);
        this.params = Map.of();
    }

    public ApiProblemException(HttpStatus status, String code, String detail) {
        super(code);
        this.status = status;
        this.code = code;
        this.detail = detail;
        this.errors = List.of();
        this.params = Map.of();
    }

    public ApiProblemException(HttpStatus status, String code, String detail, @Nullable Throwable cause) {
        super(code, cause);
        this.status = status;
        this.code = code;
        this.detail = detail;
        this.errors = List.of();
        this.params = Map.of();
    }

    public ApiProblemException(HttpStatus status, String code, String detail, List<Map<String, Object>> errors) {
        super(code);
        this.status = status;
        this.code = code;
        this.detail = detail;
        this.errors = List.copyOf(errors);
        this.params = Map.of();
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getDetail() {
        return detail;
    }

    public List<Map<String, Object>> getErrors() {
        return errors;
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
