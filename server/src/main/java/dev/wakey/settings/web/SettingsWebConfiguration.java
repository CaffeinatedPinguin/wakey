package dev.wakey.settings.web;

import dev.wakey.settings.Language;
import dev.wakey.web.OpenApiErrorResponse;
import io.swagger.v3.core.util.RefUtils;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ProblemDetail;

/** Documents the settings error answers and the language as one named schema. */
@Configuration(proxyBeanMethods = false)
class SettingsWebConfiguration {

    private static final String LANGUAGE_SCHEMA = Language.class.getSimpleName();

    private static final String LANGUAGE_REF = RefUtils.constructRef(LANGUAGE_SCHEMA);

    private static final String SETTING_SCHEMA = SpaceSettingResponse.class.getSimpleName();

    private static final String OPTIONS_PROPERTY = "options";

    private static final String INVALID_SETTINGS = "The space plan or a setting is invalid.";

    private static final String INVALID_SETTING = "The value is outside the plan or has an invalid format.";

    private static final String UNKNOWN_KEY = "The setting key is unknown.";

    private static final String FORBIDDEN = "The caller cannot change the settings of this space.";

    private static final String UNKNOWN_PLAN = "The plan is not configured on this instance.";

    private static final String NOT_ADMIN = "Only an administrator can assign a plan.";

    @Bean
    GlobalOpenApiCustomizer languageIsANamedSchema() {
        return SettingsWebConfiguration::namespaceLanguage;
    }

    @Bean
    GlobalOperationCustomizer settingsErrorResponses() {
        return (operation, handlerMethod) -> {
            if (handlerMethod.getBeanType() == SettingsController.class) {
                documentErrors(operation);
            }
            return operation;
        };
    }

    private static void documentErrors(Operation operation) {
        String operationId = operation.getOperationId();
        if (operationId == null) {
            return;
        }
        switch (operationId) {
            case "getSpaceSettings" -> addError(operation, "400", INVALID_SETTINGS);
            case "putSpaceSetting" -> {
                addError(operation, "400", INVALID_SETTING);
                addError(operation, "403", FORBIDDEN);
            }
            case "resetSpaceSettings" -> addError(operation, "403", FORBIDDEN);
            case "deleteSpaceSetting" -> {
                addError(operation, "400", UNKNOWN_KEY);
                addError(operation, "403", FORBIDDEN);
            }
            case "putSpacePlan" -> {
                addError(operation, "400", UNKNOWN_PLAN);
                addError(operation, "403", NOT_ADMIN);
            }
            default -> {
                // Every other operation documents its errors elsewhere.
            }
        }
    }

    private static void addError(Operation operation, String status, String description) {
        String errorSchemaRef = RefUtils.constructRef(ProblemDetail.class.getSimpleName());
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        responses.addApiResponse(status, OpenApiErrorResponse.of(errorSchemaRef, description));
    }

    private static void namespaceLanguage(OpenAPI openApi) {
        Components components = Objects.requireNonNullElseGet(openApi.getComponents(), Components::new);
        components.addSchemas(LANGUAGE_SCHEMA, languageSchema());
        referenceLanguage(components);
        openApi.setComponents(components);
    }

    private static void referenceLanguage(Components components) {
        Schema<?> setting = components.getSchemas().get(SETTING_SCHEMA);
        if (setting != null) {
            Map<String, Schema> properties = setting.getProperties();
            if (properties != null) {
                replaceOptions(properties);
            }
        }
    }

    private static void replaceOptions(Map<String, Schema> properties) {
        Schema<?> options = properties.get(OPTIONS_PROPERTY);
        if (options != null) {
            options.setItems(new Schema<>().$ref(LANGUAGE_REF));
        }
    }

    private static Schema<String> languageSchema() {
        List<String> values =
                Arrays.stream(Language.values()).map(Language::code).toList();
        StringSchema schema = new StringSchema();
        schema.setEnum(values);
        schema.setDescription("A supported language");
        return schema;
    }
}
