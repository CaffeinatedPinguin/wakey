package dev.wakey.web;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * The single factory for the RFC 9457 Problem Details shape: the type, the stable {@code code}, the
 * {@code errors[]} entries with their {@code params}, and the body written by the security filters.
 */
public final class Problems {

    private static final URI TYPE = URI.create("about:blank");

    private Problems() {}

    /** Builds a Problem Details response with no field errors. */
    public static ProblemDetail of(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(TYPE);
        problem.setProperty("code", code);
        problem.setProperty("errors", List.of());
        return problem;
    }

    /** Replaces the (empty) field errors of a problem. */
    public static ProblemDetail withErrors(ProblemDetail problem, List<Map<String, Object>> errors) {
        problem.setProperty("errors", errors);
        return problem;
    }

    /** One {@code errors[]} entry, pointing at the offending field. */
    public static Map<String, Object> error(String pointer, String code) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("pointer", pointer);
        entry.put("code", code);
        entry.put("params", Map.of());
        return entry;
    }

    /** The body written by the security filters, which run before MVC and cannot return a ProblemDetail. */
    public static Map<String, Object> body(HttpStatus status, String code, String detail) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", TYPE.toString());
        body.put("title", status.getReasonPhrase());
        body.put("status", status.value());
        body.put("detail", detail);
        body.put("code", code);
        body.put("errors", List.of());
        return body;
    }
}
