package dev.wakey.api.tokens;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import java.util.Objects;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documents the two ways into the public API: a bearer token and the browser session cookie. */
@Configuration(proxyBeanMethods = false)
class TokenOpenApiConfiguration {

    static final String BEARER_AUTH = "bearerAuth";

    static final String SESSION_AUTH = "sessionAuth";

    private static final String BEARER_FORMAT = "wakey API token";

    private static final String SESSION_COOKIE = "SESSION";

    private static final String TOKENS_PATH = "/api/v1/spaces/{slug}/tokens";

    private static final String TOKEN_PATH = TOKENS_PATH + "/{id}";

    @Bean
    GlobalOpenApiCustomizer securitySchemes() {
        return TokenOpenApiConfiguration::documentSchemes;
    }

    private static void documentSchemes(OpenAPI openApi) {
        Components components = Objects.requireNonNullElseGet(openApi.getComponents(), Components::new);
        components.addSecuritySchemes(BEARER_AUTH, bearerScheme());
        components.addSecuritySchemes(SESSION_AUTH, sessionScheme());
        openApi.setComponents(components);
        openApi.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
        openApi.addSecurityItem(new SecurityRequirement().addList(SESSION_AUTH));
        requireSessionForTokenManagement(openApi);
    }

    private static void requireSessionForTokenManagement(OpenAPI openApi) {
        Paths paths = Objects.requireNonNull(openApi.getPaths());
        PathItem collection = Objects.requireNonNull(paths.get(TOKENS_PATH));
        PathItem token = Objects.requireNonNull(paths.get(TOKEN_PATH));
        Operation list = Objects.requireNonNull(collection.getGet());
        Operation create = Objects.requireNonNull(collection.getPost());
        Operation revoke = Objects.requireNonNull(token.getDelete());
        List<SecurityRequirement> sessionOnly = List.of(new SecurityRequirement().addList(SESSION_AUTH));
        list.setSecurity(sessionOnly);
        create.setSecurity(sessionOnly);
        revoke.setSecurity(sessionOnly);
    }

    private static SecurityScheme bearerScheme() {
        SecurityScheme scheme = new SecurityScheme();
        scheme.setType(SecurityScheme.Type.HTTP);
        scheme.setScheme("bearer");
        scheme.setBearerFormat(BEARER_FORMAT);
        scheme.setDescription("An API token issued for one space, sent as Authorization: Bearer wk_...");
        return scheme;
    }

    private static SecurityScheme sessionScheme() {
        SecurityScheme scheme = new SecurityScheme();
        scheme.setType(SecurityScheme.Type.APIKEY);
        scheme.setIn(SecurityScheme.In.COOKIE);
        scheme.setName(SESSION_COOKIE);
        scheme.setDescription(
                "The browser session cookie; state-changing requests also send the X-XSRF-TOKEN CSRF header");
        return scheme;
    }
}
