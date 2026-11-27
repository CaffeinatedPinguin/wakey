package dev.wakey.web;

import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;

/** Documents the single {@code 401} answer that every public API operation shares. */
final class OpenApiUnauthenticatedResponse {

    private static final String UNAUTHORIZED = "401";

    private OpenApiUnauthenticatedResponse() {}

    static OperationCustomizer customizer(String errorSchemaRef) {
        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            if (responses == null) {
                responses = new ApiResponses();
                operation.setResponses(responses);
            }
            String detail = CommonError.UNAUTHENTICATED.getDetail();
            ApiResponse response = OpenApiErrorResponse.of(errorSchemaRef, detail);
            responses.addApiResponse(UNAUTHORIZED, response);
            return operation;
        };
    }
}
