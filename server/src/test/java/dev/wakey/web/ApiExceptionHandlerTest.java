package dev.wakey.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import org.junit.jupiter.api.Test;
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

class ApiExceptionHandlerTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static final String SECRET = "do-not-leak-this-detail";

    @Test
    void validationErrorsAreRenderedWithPointers() throws Exception {
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
        assertThat(code).isEqualTo(CommonError.VALIDATION_FAILED.getCode());
        assertThat(errors.isArray()).isTrue();
        assertThat(pointer).isEqualTo("/name");
    }

    @Test
    void unreadableBodyKeepsItsOwnCode() throws Exception {
        // given
        MockMvc mvc = mvcFor(new ValidationController());
        MockHttpServletRequestBuilder request = post("/api/v1/test-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{");

        // when
        MvcResult result = mvc.perform(request).andReturn();

        // then
        int status = result.getResponse().getStatus();
        String responseBody = result.getResponse().getContentAsString();
        JsonNode body = MAPPER.readTree(responseBody);
        String code = body.path("code").asString();
        JsonNode errors = body.path("errors");
        int errorCount = errors.size();
        boolean errorsAreArray = errors.isArray();
        assertThat(status).isEqualTo(400);
        assertThat(code).isEqualTo(CommonError.INVALID_BODY.getCode());
        assertThat(errorsAreArray).isTrue();
        assertThat(errorCount).isZero();
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
        assertThat(code).isEqualTo(CommonError.INTERNAL_ERROR.getCode());
        assertThat(responseBody).doesNotContain(SECRET);
    }

    @Test
    void validationErrorsCarryTheConstraintParams() throws Exception {
        // given
        MockMvc mvc = mvcFor(new BoundedController());
        MockHttpServletRequestBuilder request = post("/api/v1/bounded-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"x\",\"count\":0}");

        // when
        MvcResult result = mvc.perform(request).andReturn();

        // then
        int status = result.getResponse().getStatus();
        String responseBody = result.getResponse().getContentAsString();
        JsonNode errors = MAPPER.readTree(responseBody).path("errors");
        JsonNode codeError = errorAt(errors, "/code");
        JsonNode countError = errorAt(errors, "/count");
        JsonNode codeParams = codeError.path("params");
        JsonNode countParams = countError.path("params");
        int min = codeParams.path("min").asInt();
        int max = codeParams.path("max").asInt();
        int value = countParams.path("value").asInt();
        assertThat(status).isEqualTo(400);
        assertThat(min).isEqualTo(2);
        assertThat(max).isEqualTo(5);
        assertThat(value).isEqualTo(1);
    }

    private static JsonNode errorAt(JsonNode errors, String pointer) {
        for (JsonNode error : errors) {
            String errorPointer = error.path("pointer").asString();
            if (pointer.equals(errorPointer)) {
                return error;
            }
        }
        throw new AssertionError("Missing error for " + pointer);
    }

    @Test
    void missingResourceIsNotFound() throws Exception { // given
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
        assertThat(code).isEqualTo(CommonError.NOT_FOUND.getCode());
    }

    private static MockMvc mvcFor(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    record CreateRequest(@NotBlank String name) {}

    record BoundedRequest(
            @Size(min = 2, max = 5) String code, @Min(1) int count) {}

    @RestController
    static final class BoundedController {

        @PostMapping("/api/v1/bounded-items")
        void create(@Valid @RequestBody BoundedRequest request) {
            // The request is only validated; a valid body has nothing to do here.
        }
    }

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
            throw new ApiException(CommonError.NOT_FOUND);
        }
    }
}
