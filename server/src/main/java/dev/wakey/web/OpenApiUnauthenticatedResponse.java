package dev.wakey.web;

import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;

/** Documents the single {@code 401} answer that every public API operation shares. */
final class OpenApiUnauthenticatedResponse {

    private static final String UNAUTHORIZED = "401";

    private OpenApiUnauthenticatedResponse() {}

    static OperationCustomizer customizer(String problemDetailRef) {
        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            if (responses == null) {
                responses = new ApiResponses();
                operation.setResponses(responses);
            }
            responses.addApiResponse(UNAUTHORIZED, problemResponse(problemDetailRef));
            return operation;
        };
    }

    private static ApiResponse problemResponse(String problemDetailRef) {
        Schema<?> schema = new Schema<>().$ref(problemDetailRef);
        MediaType mediaType = new MediaType().schema(schema);
        Content content = new Content();
        content.addMediaType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE, mediaType);
        return new ApiResponse()
                .description(CommonError.UNAUTHENTICATED.getDetail())
                .content(content);
    }
}
