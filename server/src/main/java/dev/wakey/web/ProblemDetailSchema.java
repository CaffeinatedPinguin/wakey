package dev.wakey.web;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.List;

/** Builds the {@code ProblemDetail} schema of the public contract, including every known error code. */
final class ProblemDetailSchema {

    private ProblemDetailSchema() {}

    static Schema<?> schema(List<String> codes) {
        ObjectSchema fieldError = new ObjectSchema();
        fieldError.addProperty("pointer", new StringSchema());
        fieldError.addProperty("code", new StringSchema());
        fieldError.addProperty("params", new ObjectSchema().additionalProperties(true));
        ArraySchema errors = new ArraySchema();
        errors.setItems(fieldError);
        StringSchema code = new StringSchema();
        code.setEnum(codes);
        ObjectSchema schema = new ObjectSchema();
        schema.addProperty("type", new StringSchema().format("uri"));
        schema.addProperty("title", new StringSchema());
        schema.addProperty("status", new IntegerSchema().format("int32"));
        schema.addProperty("detail", new StringSchema());
        schema.addProperty("instance", new StringSchema().format("uri"));
        schema.addProperty("code", code);
        schema.addProperty("errors", errors);
        return schema;
    }
}
