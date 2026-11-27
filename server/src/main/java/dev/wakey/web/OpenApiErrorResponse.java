package dev.wakey.web;

import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;

/** Builds one {@code application/problem+json} response of the public contract. */
public final class OpenApiErrorResponse {

    private OpenApiErrorResponse() {}

    public static ApiResponse of(String errorSchemaRef, String description) {
        Schema<?> schema = new Schema<>().$ref(errorSchemaRef);
        MediaType mediaType = new MediaType().schema(schema);
        Content content = new Content();
        content.addMediaType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE, mediaType);
        return new ApiResponse().description(description).content(content);
    }
}
