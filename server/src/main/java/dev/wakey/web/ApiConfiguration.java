package dev.wakey.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
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

    private static final String SPACES_PATH = ApiPaths.API_V1 + "/spaces";

    private static final String SPACES_ITEM_PATH = SPACES_PATH + "/{slug}";

    private static final String LIMIT_PARAMETER = "limit";

    private static final String IF_NONE_MATCH = "If-None-Match";

    private static final String IF_MATCH = "If-Match";

    private static final String SPACES_TAG = "spaces-controller";

    @Bean
    GroupedOpenApi publicApi(ApiProperties apiProperties) {
        return GroupedOpenApi.builder()
                .group(GROUP)
                .pathsToMatch(ApiPaths.API_V1_ALL)
                .addOpenApiCustomizer(openApi -> documentPublicApi(openApi, apiProperties))
                .build();
    }

    private static void documentPublicApi(OpenAPI openApi, ApiProperties apiProperties) {
        Info info = new Info().title(TITLE).version(VERSION);
        openApi.setInfo(info);
        Components components = Objects.requireNonNullElseGet(openApi.getComponents(), Components::new);
        components.addSchemas(PROBLEM_DETAIL_SCHEMA, problemDetailSchema());
        openApi.setComponents(components);
        Paths paths = openApi.getPaths();
        documentPageLimit(paths, apiProperties.maxPageSize());
        documentPreconditionHeaders(paths);
    }

    private static void documentPageLimit(@Nullable Paths paths, int maxPageSize) {
        if (paths != null) {
            PathItem spaces = paths.get(SPACES_PATH);
            if (spaces != null) {
                documentPageBounds(spaces.getGet(), maxPageSize);
            }
        }
    }

    private static void documentPageBounds(@Nullable Operation operation, int maxPageSize) {
        if (operation != null) {
            List<Parameter> parameters = operation.getParameters();
            if (parameters != null) {
                parameters.stream()
                        .filter(ApiConfiguration::isLimit)
                        .findFirst()
                        .ifPresent(parameter -> setPageBounds(parameter, maxPageSize));
            }
        }
    }

    private static void setPageBounds(Parameter parameter, int maxPageSize) {
        Schema<?> schema = parameter.getSchema();
        if (schema != null) {
            schema.setMinimum(BigDecimal.ONE);
            schema.setMaximum(BigDecimal.valueOf(maxPageSize));
        }
    }

    private static boolean isLimit(Parameter parameter) {
        return LIMIT_PARAMETER.equals(parameter.getName());
    }

    private static void documentPreconditionHeaders(@Nullable Paths paths) {
        if (paths != null) {
            PathItem item = paths.get(SPACES_ITEM_PATH);
            if (item != null) {
                addPreconditionHeaders(item.getPut());
            }
        }
    }

    private static void addPreconditionHeaders(@Nullable Operation operation) {
        if (operation != null) {
            operation.setTags(List.of(SPACES_TAG));
            List<Parameter> parameters = operation.getParameters();
            if (parameters != null) {
                Parameter ifNoneMatch =
                        headerParameter(IF_NONE_MATCH, "Send * to create the space only when the slug is free.");
                Parameter ifMatch = headerParameter(IF_MATCH, "The quoted space version to rename, for example \"1\".");
                parameters.add(ifNoneMatch);
                parameters.add(ifMatch);
            }
        }
    }

    private static Parameter headerParameter(String name, String description) {
        Parameter parameter = new Parameter();
        parameter.setIn("header");
        parameter.setName(name);
        parameter.setRequired(false);
        parameter.setDescription(description);
        parameter.setSchema(new StringSchema());
        return parameter;
    }

    private static Schema<?> problemDetailSchema() {
        ObjectSchema fieldError = new ObjectSchema();
        fieldError.addProperty("pointer", new StringSchema());
        fieldError.addProperty("code", new StringSchema());
        fieldError.addProperty("params", new ObjectSchema().additionalProperties(true));
        ArraySchema errors = new ArraySchema();
        errors.setItems(fieldError);
        ObjectSchema schema = new ObjectSchema();
        schema.addProperty("type", new StringSchema().format("uri"));
        schema.addProperty("title", new StringSchema());
        schema.addProperty("status", new IntegerSchema().format("int32"));
        schema.addProperty("detail", new StringSchema());
        schema.addProperty("instance", new StringSchema().format("uri"));
        schema.addProperty("code", new StringSchema());
        schema.addProperty("errors", errors);
        return schema;
    }
}
