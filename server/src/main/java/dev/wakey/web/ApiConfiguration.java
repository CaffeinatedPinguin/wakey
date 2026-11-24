package dev.wakey.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.Objects;
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

    @Bean
    GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group(GROUP)
                .pathsToMatch(ApiPaths.API_V1_ALL)
                .addOpenApiCustomizer(ApiConfiguration::documentPublicApi)
                .build();
    }

    private static void documentPublicApi(OpenAPI openApi) {
        Info info = new Info().title(TITLE).version(VERSION);
        openApi.setInfo(info);
        Components components = Objects.requireNonNullElseGet(openApi.getComponents(), Components::new);
        components.addSchemas(PROBLEM_DETAIL_SCHEMA, problemDetailSchema());
        openApi.setComponents(components);
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
