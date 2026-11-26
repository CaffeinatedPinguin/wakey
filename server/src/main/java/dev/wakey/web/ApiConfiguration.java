package dev.wakey.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.util.ArrayList;
import java.util.List;
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

    private static final String ERROR_SCHEMA_NAME = "ProblemDetail";

    private static final String ERROR_SCHEMA_REF = "#/components/schemas/" + ERROR_SCHEMA_NAME;

    @Bean
    GroupedOpenApi publicApi(ApiProperties apiProperties, List<ErrorCodeSet> errorCodeSets) {
        List<String> codes = errorCodeValues(errorCodeSets);
        return GroupedOpenApi.builder()
                .group(GROUP)
                .pathsToMatch(ApiPaths.API_V1_ALL)
                .addOpenApiCustomizer(openApi -> documentPublicApi(openApi, apiProperties, codes))
                .addOperationCustomizer(OpenApiUnauthenticatedResponse.customizer(ERROR_SCHEMA_REF))
                .build();
    }

    private static void documentPublicApi(OpenAPI openApi, ApiProperties apiProperties, List<String> codes) {
        Info info = new Info().title(TITLE).version(VERSION);
        openApi.setInfo(info);
        Components components = openApi.getComponents();
        if (components == null) {
            components = new Components();
        }
        components.addSchemas(ERROR_SCHEMA_NAME, ApiExceptionSchema.schema(codes));
        openApi.setComponents(components);
        OpenApiPageLimit.document(openApi.getPaths(), apiProperties.maxPageSize());
    }

    private static List<String> errorCodeValues(List<ErrorCodeSet> errorCodeSets) {
        List<String> codes = new ArrayList<>();
        for (CommonError error : CommonError.values()) {
            codes.add(error.getCode());
        }
        for (ErrorCodeSet errorCodeSet : errorCodeSets) {
            for (ErrorCode error : errorCodeSet.codes()) {
                codes.add(error.getCode());
            }
        }
        return List.copyOf(codes);
    }
}
