package dev.wakey.api.tokens;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Objects;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documents the {@code Authorization: Bearer wk_...} scheme accepted by the public API. */
@Configuration(proxyBeanMethods = false)
class TokenOpenApiConfiguration {

    private static final String BEARER_AUTH = "bearerAuth";

    private static final String BEARER_FORMAT = "wakey API token";

    @Bean
    GlobalOpenApiCustomizer bearerSecurityScheme() {
        return TokenOpenApiConfiguration::documentBearer;
    }

    private static void documentBearer(OpenAPI openApi) {
        Components components = Objects.requireNonNullElseGet(openApi.getComponents(), Components::new);
        components.addSecuritySchemes(BEARER_AUTH, securityScheme());
        openApi.setComponents(components);
        openApi.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    private static SecurityScheme securityScheme() {
        SecurityScheme scheme = new SecurityScheme();
        scheme.setType(SecurityScheme.Type.HTTP);
        scheme.setScheme("bearer");
        scheme.setBearerFormat(BEARER_FORMAT);
        scheme.setDescription("An API token issued for one space, sent as Authorization: Bearer wk_...");
        return scheme;
    }
}
