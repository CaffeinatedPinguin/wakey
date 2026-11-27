package dev.wakey.checks.web;

import dev.wakey.spaces.SpaceSlug;
import dev.wakey.web.OpenApiErrorResponse;
import io.swagger.v3.core.util.RefUtils;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ProblemDetail;

/** Documents the checks operations, their named enum schemas and the shared {@code {slug}} path. */
@Configuration(proxyBeanMethods = false)
class ChecksWebConfiguration {

    private static final Map<String, String> ENUM_SCHEMAS =
            Map.of("state", "CheckState", "type", "CheckType", "pausedReason", "PausedReason");

    private static final Set<String> READ_OPERATIONS = Set.of("listChecks", "countChecks", "getCheck");

    private static final String ERROR_SCHEMA_REF = RefUtils.constructRef(ProblemDetail.class.getSimpleName());

    private static final String SLUG_PARAMETER = "slug";

    private static final String PATH_LOCATION = "path";

    private static final String CHECKS_TAG = "checks";

    private static final String INVALID_REQUEST = "The request is invalid.";

    private static final String FORBIDDEN = "The role does not allow changing checks in this space.";

    private static final String INVALID_CURSOR = "The cursor is invalid.";

    private static final String CONFLICT = "The check was changed by someone else.";

    private static final String COUNT_CHANGED = "The number of matching checks changed since the dialog was shown.";

    @Bean
    GlobalOperationCustomizer checksOperations() {
        return (operation, handlerMethod) -> {
            if (handlerMethod.getBeanType() == ChecksController.class) {
                operation.setTags(List.of(CHECKS_TAG));
                document(operation);
                documentPathParameters(operation);
                referenceEnumSchemas(operation);
            }
            return operation;
        };
    }

    private static void document(Operation operation) {
        String operationId = operation.getOperationId();
        if (operationId != null) {
            if (READ_OPERATIONS.contains(operationId)) {
                documentRead(operation, operationId);
            } else {
                documentWrite(operation, operationId);
            }
        }
    }

    private static void documentRead(Operation operation, String operationId) {
        switch (operationId) {
            case "listChecks" -> documentList(operation);
            case "countChecks" -> documentCount(operation);
            case "getCheck" -> documentGet(operation);
            default -> {
                // Every other operation writes.
            }
        }
    }

    private static void documentWrite(Operation operation, String operationId) {
        switch (operationId) {
            case "createCheck" -> documentCreate(operation);
            case "updateCheck" -> documentUpdate(operation);
            case "pauseCheck" -> documentPause(operation);
            case "resumeCheck" -> documentResume(operation);
            case "deleteCheck" -> documentDelete(operation);
            case "bulkChecks" -> documentBulk(operation);
            default -> {
                // Every other operation is not part of the checks resource.
            }
        }
    }

    private static void documentList(Operation operation) {
        operation.setSummary("List the checks of a space");
        addSuccess(operation, "CursorPageCheckResponse");
        addError(operation, "400", INVALID_CURSOR);
    }

    private static void documentCount(Operation operation) {
        operation.setSummary("Count the checks of a space by state");
        addSuccess(operation, "StateCountsResponse");
    }

    private static void documentGet(Operation operation) {
        operation.setSummary("Read one check of a space with its newest result");
    }

    private static void documentCreate(Operation operation) {
        operation.setSummary("Create a check in a space");
        addError(operation, "400", INVALID_REQUEST);
        addError(operation, "403", FORBIDDEN);
    }

    private static void documentUpdate(Operation operation) {
        operation.setSummary("Replace a check, rejecting a stale version");
        addError(operation, "400", INVALID_REQUEST);
        addError(operation, "403", FORBIDDEN);
        addError(operation, "409", CONFLICT);
    }

    private static void documentPause(Operation operation) {
        operation.setSummary("Pause monitoring of a check");
        addError(operation, "400", INVALID_REQUEST);
        addError(operation, "403", FORBIDDEN);
    }

    private static void documentResume(Operation operation) {
        operation.setSummary("Resume monitoring of a check");
        addError(operation, "403", FORBIDDEN);
    }

    private static void documentDelete(Operation operation) {
        operation.setSummary("Delete a check together with its results and tags");
        addError(operation, "403", FORBIDDEN);
    }

    private static void documentBulk(Operation operation) {
        operation.setSummary("Pause, resume or delete many checks at once");
        addSuccess(operation, "BulkChecksResponse");
        addError(operation, "400", INVALID_REQUEST);
        addError(operation, "403", FORBIDDEN);
        addError(operation, "409", COUNT_CHANGED);
    }

    private static void addSuccess(Operation operation, String schemaName) {
        Schema<?> schema = new Schema<>().$ref(RefUtils.constructRef(schemaName));
        MediaType mediaType = new MediaType().schema(schema);
        Content content = new Content();
        content.addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE, mediaType);
        ApiResponse response = new ApiResponse().description("OK").content(content);
        responses(operation).addApiResponse("200", response);
    }

    private static void addError(Operation operation, String status, String description) {
        responses(operation).addApiResponse(status, OpenApiErrorResponse.of(ERROR_SCHEMA_REF, description));
    }

    private static ApiResponses responses(Operation operation) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        return responses;
    }

    private static void documentPathParameters(Operation operation) {
        List<Parameter> parameters = operation.getParameters();
        if (parameters == null) {
            return;
        }
        for (Parameter parameter : parameters) {
            if (isPath(parameter, SLUG_PARAMETER)) {
                setPattern(parameter);
            }
        }
    }

    private static boolean isPath(Parameter parameter, String name) {
        return name.equals(parameter.getName()) && PATH_LOCATION.equals(parameter.getIn());
    }

    private static void setPattern(Parameter parameter) {
        Schema<?> schema = parameter.getSchema();
        if (schema != null) {
            StringSchema copy = new StringSchema();
            copy.setPattern(SpaceSlug.PATTERN);
            parameter.setSchema(copy);
        }
    }

    private static void referenceEnumSchemas(Operation operation) {
        List<Parameter> parameters = operation.getParameters();
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                referenceEnumSchema(parameter);
            }
        }
    }

    private static void referenceEnumSchema(Parameter parameter) {
        String schemaName = ENUM_SCHEMAS.get(parameter.getName());
        Schema<?> schema = parameter.getSchema();
        if (schemaName != null && schema != null) {
            schema.setEnum(null);
            schema.set$ref(RefUtils.constructRef(schemaName));
        }
    }
}
