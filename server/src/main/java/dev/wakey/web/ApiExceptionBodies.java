package dev.wakey.web;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ProblemDetail;

/** The single factory for the RFC 9457 error body shape: type, code and field errors. */
public final class ApiExceptionBodies {

    private static final URI TYPE = URI.create("about:blank");

    private ApiExceptionBodies() {}

    public static ProblemDetail of(ErrorCode error) {
        return of(error, List.of());
    }

    public static ProblemDetail of(ErrorCode error, List<ApiFieldError> errors) {
        ProblemDetail body = ProblemDetail.forStatus(error.getStatus());
        return apply(body, error, errors);
    }

    public static ProblemDetail apply(ProblemDetail body, ErrorCode error) {
        return apply(body, error, List.of());
    }

    private static ProblemDetail apply(ProblemDetail body, ErrorCode error, List<ApiFieldError> errors) {
        body.setType(TYPE);
        body.setDetail(error.getDetail());
        body.setProperty("code", error.getCode());
        body.setProperty("errors", entries(errors));
        return body;
    }

    private static List<Map<String, Object>> entries(List<ApiFieldError> errors) {
        List<Map<String, Object>> entries = new ArrayList<>(errors.size());
        for (ApiFieldError error : errors) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("pointer", error.pointer());
            entry.put("code", error.code());
            entry.put("params", error.params());
            entries.add(entry);
        }
        return entries;
    }
}
