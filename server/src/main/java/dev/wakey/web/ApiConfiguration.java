package dev.wakey.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers the API limits and the public OpenAPI group generated from the controllers. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ApiProperties.class)
public class ApiConfiguration {

    private static final String GROUP = "public";

    private static final String TITLE = "wakey API";

    private static final String VERSION = "v1";

    private static final String PROBLEM_DETAIL_SCHEMA = "ProblemDetail";

    private static final String PROBLEM_DETAIL_REF = "#/components/schemas/" + PROBLEM_DETAIL_SCHEMA;

    @Bean
    GroupedOpenApi publicApi(ApiProperties apiProperties, List<ErrorCodes> errorCodeGroups) {
        List<String> codes = errorCodeValues(errorCodeGroups);
        return GroupedOpenApi.builder()
                .group(GROUP)
                .pathsToMatch(ApiPaths.API_V1_ALL)
                .addOpenApiCustomizer(openApi -> documentPublicApi(openApi, apiProperties, codes))
                .addOperationCustomizer(OpenApiUnauthenticatedResponse.customizer(PROBLEM_DETAIL_REF))
                .build();
    }

    private static void documentPublicApi(OpenAPI openApi, ApiProperties apiProperties, List<String> codes) {
        Info info = new Info().title(TITLE).version(VERSION);
        openApi.setInfo(info);
        Components components = openApi.getComponents();
        if (components == null) {
            components = new Components();
        }
        components.addSchemas(PROBLEM_DETAIL_SCHEMA, ProblemDetailSchema.schema(codes));
        openApi.setComponents(components);
        OpenApiPageLimit.document(openApi.getPaths(), apiProperties.maxPageSize());
    }

    private static List<String> errorCodeValues(List<ErrorCodes> errorCodeGroups) {
        List<String> codes = new ArrayList<>();
        for (CommonError error : CommonError.values()) {
            codes.add(error.getCode());
        }
        for (ErrorCodes group : errorCodeGroups) {
            for (ErrorCode error : group.codes()) {
                codes.add(error.getCode());
            }
        }
        return List.copyOf(codes);
    }

    /** Documents the single {@code 401} answer that every public API operation shares. */
    private static final class OpenApiUnauthenticatedResponse {

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

    /** Documents the instance page-size bounds on the {@code limit} parameter of every operation. */
    private static final class OpenApiPageLimit {

        private static final String LIMIT_PARAMETER = "limit";

        private OpenApiPageLimit() {}

        static void document(@Nullable Paths paths, int maxPageSize) {
            if (paths != null) {
                for (PathItem pathItem : paths.values()) {
                    documentBounds(pathItem.readOperations(), maxPageSize);
                }
            }
        }

        private static void documentBounds(List<Operation> operations, int maxPageSize) {
            for (Operation operation : operations) {
                List<Parameter> parameters = operation.getParameters();
                if (parameters != null) {
                    parameters.stream()
                            .filter(OpenApiPageLimit::isLimit)
                            .findFirst()
                            .ifPresent(parameter -> setBounds(parameter, maxPageSize));
                }
            }
        }

        private static void setBounds(Parameter parameter, int maxPageSize) {
            Schema<?> schema = Objects.requireNonNull(parameter.getSchema());
            schema.setMinimum(BigDecimal.ONE);
            schema.setMaximum(BigDecimal.valueOf(maxPageSize));
        }

        private static boolean isLimit(Parameter parameter) {
            return LIMIT_PARAMETER.equals(parameter.getName());
        }
    }
}
