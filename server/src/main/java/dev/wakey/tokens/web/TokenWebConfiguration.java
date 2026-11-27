package dev.wakey.tokens.web;

import dev.wakey.tokens.TokenScope;
import dev.wakey.tokens.TokenSummary;
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

/** Documents the token scope as one named schema and references it from the token summary. */
@Configuration(proxyBeanMethods = false)
class TokenWebConfiguration {

    private static final String TOKEN_SCOPE_SCHEMA = TokenScope.class.getSimpleName();

    private static final String TOKEN_SCOPE_REF = RefUtils.constructRef(TOKEN_SCOPE_SCHEMA);

    private static final String TOKEN_SUMMARY_SCHEMA = TokenSummary.class.getSimpleName();

    private static final String SCOPE_PROPERTY = "scope";

    @Bean
    GlobalOpenApiCustomizer tokenScopeIsANamedSchema() {
        return TokenWebConfiguration::namespaceTokenScope;
    }

    private static void namespaceTokenScope(OpenAPI openApi) {
        Components components = Objects.requireNonNullElseGet(openApi.getComponents(), Components::new);
        components.addSchemas(TOKEN_SCOPE_SCHEMA, tokenScopeSchema());
        referenceTokenScope(components);
        openApi.setComponents(components);
    }

    private static void referenceTokenScope(Components components) {
        Schema<?> summary = components.getSchemas().get(TOKEN_SUMMARY_SCHEMA);
        if (summary != null) {
            Map<String, Schema> properties = summary.getProperties();
            if (properties != null) {
                properties.put(SCOPE_PROPERTY, new Schema<>().$ref(TOKEN_SCOPE_REF));
            }
        }
    }

    private static Schema<String> tokenScopeSchema() {
        List<String> values =
                Arrays.stream(TokenScope.values()).map(TokenScope::name).toList();
        StringSchema schema = new StringSchema();
        schema.setEnum(values);
        return schema;
    }
}
