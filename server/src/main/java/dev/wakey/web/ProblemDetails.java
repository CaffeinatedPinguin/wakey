package dev.wakey.web;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/** The single factory for the RFC 9457 Problem Details shape: type, code and field errors. */
public final class ProblemDetails {

    private static final URI TYPE = URI.create("about:blank");

    private ProblemDetails() {}

    public static ProblemDetail of(ErrorCode error) {
        return of(error, List.of());
    }

    public static ProblemDetail of(ErrorCode error, List<FieldProblem> errors) {
        return problem(error.getStatus(), error.getDetail(), error.getCode(), errors);
    }

    private static ProblemDetail problem(HttpStatusCode status, String detail, String code, List<FieldProblem> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(TYPE);
        problem.setProperty("code", code);
        problem.setProperty("errors", entries(errors));
        return problem;
    }

    private static List<Map<String, Object>> entries(List<FieldProblem> errors) {
        List<Map<String, Object>> entries = new ArrayList<>(errors.size());
        for (FieldProblem error : errors) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("pointer", error.pointer());
            entry.put("code", error.code());
            entry.put("params", error.params());
            entries.add(entry);
        }
        return entries;
    }
}
