package dev.wakey.spaces.web;

import dev.wakey.spaces.SpaceError;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceSlug;
import dev.wakey.web.ErrorCodeSet;
import io.swagger.v3.core.util.RefUtils;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers the spaces error codes, the shared {@code {slug}} parameter and the domain schema tweaks. */
@Configuration(proxyBeanMethods = false)
class SpacesWebConfiguration {

    private static final String SPACE_RESPONSE_SCHEMA = "SpaceResponse";

    private static final String SPACE_ROLE_SCHEMA = SpaceRole.class.getSimpleName();

    private static final String SPACE_ROLE_REF = RefUtils.constructRef(SPACE_ROLE_SCHEMA);

    private static final String ROLE_PROPERTY = "role";

    private static final String SLUG_PARAMETER = "slug";

    private static final String PATH_LOCATION = "path";

    @Bean
    ErrorCodeSet spaceErrorCodes() {
        List<SpaceError> codes = List.of(SpaceError.values());
        return new ErrorCodeSet(codes);
    }

    @Bean
    GlobalOpenApiCustomizer spaceResponseRoleIsNullable() {
        return SpacesWebConfiguration::makeSpaceResponseRoleNullable;
    }

    @Bean
    GlobalOperationCustomizer slugParameterPattern() {
        return (operation, handlerMethod) -> {
            documentSlugPattern(operation);
            return operation;
        };
    }

    private static void documentSlugPattern(Operation operation) {
        List<Parameter> parameters = operation.getParameters();
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                setSlugPattern(parameter);
            }
        }
    }

    private static void setSlugPattern(Parameter parameter) {
        Schema<?> schema = parameter.getSchema();
        if (isSlugPath(parameter) && schema != null) {
            schema.setPattern(SpaceSlug.PATTERN);
        }
    }

    private static boolean isSlugPath(Parameter parameter) {
        return SLUG_PARAMETER.equals(parameter.getName()) && PATH_LOCATION.equals(parameter.getIn());
    }

    private static void makeSpaceResponseRoleNullable(OpenAPI openApi) {
        Optional.ofNullable(openApi.getComponents())
                .map(Components::getSchemas)
                .map(schemas -> (Schema<?>) schemas.get(SPACE_RESPONSE_SCHEMA))
                .map(Schema::getProperties)
                .map(properties -> (Schema<?>) properties.get(ROLE_PROPERTY))
                .ifPresent(SpacesWebConfiguration::makeNullable);
    }

    private static void makeNullable(Schema<?> role) {
        role.set$ref(null);
        role.setTypes(null);
        Schema<?> nullType = new Schema<>();
        nullType.addType("null");
        List<Schema> alternatives = new ArrayList<>();
        alternatives.add(new Schema<>().$ref(SPACE_ROLE_REF));
        alternatives.add(nullType);
        role.setAnyOf(alternatives);
    }
}
