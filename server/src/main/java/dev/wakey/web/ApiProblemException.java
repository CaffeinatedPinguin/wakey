package dev.wakey.web;

import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;

/**
 * A domain error that the HTTP layer renders as an RFC 9457 Problem Details response. Modules throw it
 * without knowing about Spring MVC.
 */
public class ApiProblemException extends RuntimeException {

    private final HttpStatus status;

    private final String code;

    private final Map<String, Object> params;

    public ApiProblemException(HttpStatus status, String code) {
        this(status, code, Map.of(), null);
    }

    public ApiProblemException(HttpStatus status, String code, Map<String, Object> params) {
        this(status, code, params, null);
    }

    public ApiProblemException(HttpStatus status, String code, Map<String, Object> params, @Nullable Throwable cause) {
        super(code, cause);
        this.status = status;
        this.code = code;
        this.params = Map.copyOf(params);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
