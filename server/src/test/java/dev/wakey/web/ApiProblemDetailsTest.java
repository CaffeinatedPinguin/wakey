package dev.wakey.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class ApiProblemDetailsTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static final String SECRET = "do-not-leak-this-detail";

    @Test
    void validationErrorsAreProblemDetailsWithPointers() throws Exception {
        // given
        MockMvc mvc = mvcFor(new ValidationController());
        MockHttpServletRequestBuilder request = post("/api/v1/test-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}");

        // when
        MvcResult result = mvc.perform(request).andReturn();

        // then
        int status = result.getResponse().getStatus();
        String rawContentType = result.getResponse().getContentType();
        String contentType = Objects.requireNonNull(rawContentType);
        String responseBody = result.getResponse().getContentAsString();
        JsonNode body = MAPPER.readTree(responseBody);
        JsonNode errors = body.path("errors");
        String code = body.path("code").asString();
        String pointer = errors.path(0).path("pointer").asString();
        assertThat(status).isEqualTo(400);
        assertThat(contentType).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(code).isEqualTo("validation_failed");
        assertThat(errors.isArray()).isTrue();
        assertThat(pointer).isEqualTo("/name");
    }

    @Test
    void unknownErrorsDoNotLeakDetails() throws Exception {
        // given
        MockMvc mvc = mvcFor(new FailingController());

        // when
        MvcResult result = mvc.perform(get("/api/v1/test-failure")).andReturn();

        // then
        int status = result.getResponse().getStatus();
        String rawContentType = result.getResponse().getContentType();
        String contentType = Objects.requireNonNull(rawContentType);
        String responseBody = result.getResponse().getContentAsString();
        JsonNode body = MAPPER.readTree(responseBody);
        String code = body.path("code").asString();
        assertThat(status).isEqualTo(500);
        assertThat(contentType).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(code).isEqualTo("internal_error");
        assertThat(responseBody).doesNotContain(SECRET);
    }

    @Test
    void missingResourceIsProblemNotFound() throws Exception {
        // given
        MockMvc mvc = mvcFor(new MissingController());

        // when
        MvcResult result = mvc.perform(get("/api/v1/test-missing")).andReturn();

        // then
        int status = result.getResponse().getStatus();
        String rawContentType = result.getResponse().getContentType();
        String contentType = Objects.requireNonNull(rawContentType);
        String responseBody = result.getResponse().getContentAsString();
        JsonNode body = MAPPER.readTree(responseBody);
        String code = body.path("code").asString();
        assertThat(status).isEqualTo(404);
        assertThat(contentType).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(code).isEqualTo("not_found");
    }

    private static MockMvc mvcFor(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    record CreateRequest(@NotBlank String name) {}

    @RestController
    static final class ValidationController {

        @PostMapping("/api/v1/test-items")
        void create(@Valid @RequestBody CreateRequest request) {
            // The request is only validated; a valid body has nothing to do here.
        }
    }

    @RestController
    static final class FailingController {

        @GetMapping("/api/v1/test-failure")
        void fail() {
            throw new IllegalStateException(SECRET);
        }
    }

    @RestController
    static final class MissingController {

        @GetMapping("/api/v1/test-missing")
        void missing() {
            throw new ApiProblemException(HttpStatus.NOT_FOUND, "not_found");
        }
    }
}
