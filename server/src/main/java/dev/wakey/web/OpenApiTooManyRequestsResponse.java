package dev.wakey.web;

import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;

/** Documents the single {@code 429} answer that every public API operation shares. */
final class OpenApiTooManyRequestsResponse {

    private static final String TOO_MANY_REQUESTS = "429";

    private OpenApiTooManyRequestsResponse() {}

    static OperationCustomizer customizer(String errorSchemaRef) {
        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            if (responses == null) {
                responses = new ApiResponses();
                operation.setResponses(responses);
            }
            String detail = CommonError.TOO_MANY_REQUESTS.getDetail();
            ApiResponse response = OpenApiErrorResponse.of(errorSchemaRef, detail);
            response.addHeaderObject("Retry-After", retryAfterHeader());
            responses.addApiResponse(TOO_MANY_REQUESTS, response);
            return operation;
        };
    }

    private static Header retryAfterHeader() {
        StringSchema schema = new StringSchema();
        return new Header().description("Seconds to wait before retrying.").schema(schema);
    }
}
