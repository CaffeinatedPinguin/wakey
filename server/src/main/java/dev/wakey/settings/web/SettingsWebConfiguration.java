package dev.wakey.settings.web;

import dev.wakey.settings.Language;
import io.swagger.v3.core.util.RefUtils;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documents the settings language as one named schema. */
@Configuration(proxyBeanMethods = false)
class SettingsWebConfiguration {

    private static final String LANGUAGE_SCHEMA = Language.class.getSimpleName();

    private static final String LANGUAGE_REF = RefUtils.constructRef(LANGUAGE_SCHEMA);

    private static final String SETTING_SCHEMA = SpaceSettingResponse.class.getSimpleName();

    private static final String OPTIONS_PROPERTY = "options";

    @Bean
    GlobalOpenApiCustomizer languageIsANamedSchema() {
        return SettingsWebConfiguration::namespaceLanguage;
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
