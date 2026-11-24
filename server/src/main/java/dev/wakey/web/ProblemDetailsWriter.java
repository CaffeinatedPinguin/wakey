package dev.wakey.web;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes an RFC 9457 Problem Details body for the security handlers, which run before Spring MVC and
 * therefore cannot use the MVC exception advice to return a {@code ProblemDetail}.
 */
public final class ProblemDetailsWriter {

    private static final String PROBLEM_TYPE = "about:blank";

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private ProblemDetailsWriter() {}

    public static void write(HttpServletResponse response, HttpStatus status, String code, String detail)
            throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", PROBLEM_TYPE);
        body.put("title", status.getReasonPhrase());
        body.put("status", status.value());
        body.put("detail", detail);
        body.put("code", code);
        body.put("errors", List.of());
        byte[] bytes = MAPPER.writeValueAsBytes(body);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getOutputStream().write(bytes);
    }
}
