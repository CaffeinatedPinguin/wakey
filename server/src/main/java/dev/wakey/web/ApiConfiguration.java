package dev.wakey.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    private static final String SPACE_ROLE_SCHEMA = "SpaceRole";

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
        markSpaceRoleNullable(components);
        openApi.setComponents(components);
        OpenApiPageLimit.document(openApi.getPaths(), apiProperties.maxPageSize());
    }

    private static void markSpaceRoleNullable(Components components) {
        Map<String, Schema> schemas = components.getSchemas();
        if (schemas != null) {
            Schema<?> role = schemas.get(SPACE_ROLE_SCHEMA);
            if (role != null) {
                role.addType("null");
            }
        }
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
}
