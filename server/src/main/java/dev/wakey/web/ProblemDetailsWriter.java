package dev.wakey.web;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes an RFC 9457 Problem Details body for the security handlers, which run before Spring MVC and
 * therefore cannot use the MVC exception advice to return a {@code ProblemDetail}.
 */
public final class ProblemDetailsWriter {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)
            .build();

    private ProblemDetailsWriter() {}

    public static void write(HttpServletResponse response, ErrorCode error) throws IOException {
        ProblemDetail problem = ProblemDetails.of(error);
        byte[] bytes = MAPPER.writeValueAsBytes(problem);
        int status = error.getStatus().value();
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getOutputStream().write(bytes);
    }
}
