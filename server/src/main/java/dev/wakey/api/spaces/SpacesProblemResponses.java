package dev.wakey.api.spaces;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

/** Documents the shared Problem Details responses of the spaces endpoints in the public contract. */
@Component
final class SpacesProblemResponses implements GlobalOperationCustomizer {

    private static final String PROBLEM_MEDIA_TYPE = "application/problem+json";

    private static final String OK_MEDIA_TYPE = "*/*";

    private static final String PROBLEM_SCHEMA = "#/components/schemas/ProblemDetail";

    private static final String SPACE_SCHEMA = "#/components/schemas/SpaceResponse";

    private static final String PUT_SPACE = "putSpace";

    private static final String BAD_REQUEST = "The request is invalid.";

    private static final String FORBIDDEN = "The caller is not allowed to perform this action.";

    private static final String NOT_FOUND = "The requested space was not found.";

    private static final String CREATED = "The space was created.";

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        if (handlerMethod.getBeanType() != SpacesController.class) {
            return operation;
        }
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        responses.addApiResponse("400", problem(BAD_REQUEST));
        responses.addApiResponse("403", problem(FORBIDDEN));
        responses.addApiResponse("404", problem(NOT_FOUND));
        String methodName = handlerMethod.getMethod().getName();
        if (PUT_SPACE.equals(methodName)) {
            responses.addApiResponse("201", created());
        }
        return operation;
    }

    private static ApiResponse created() {
        Schema<?> schema = new Schema<>();
        schema.$ref(SPACE_SCHEMA);
        Content content = new Content();
        content.addMediaType(OK_MEDIA_TYPE, new MediaType().schema(schema));
        return new ApiResponse().description(CREATED).content(content);
    }

    private static ApiResponse problem(String description) {
        Schema<?> schema = new Schema<>();
        schema.$ref(PROBLEM_SCHEMA);
        Content content = new Content();
        content.addMediaType(PROBLEM_MEDIA_TYPE, new MediaType().schema(schema));
        return new ApiResponse().description(description).content(content);
    }
}
