package dev.wakey.spaces.web;

import dev.wakey.spaces.SpaceError;
import dev.wakey.spaces.SpaceSlug;
import dev.wakey.web.ErrorCodeSet;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

/** Registers the spaces error codes, parameter docs and the domain schema tweaks of the contract. */
@Configuration(proxyBeanMethods = false)
class SpacesWebConfiguration {

    private static final String SPACE_ROLE_SCHEMA = "SpaceRole";

    private static final String LIMIT_PARAMETER = "limit";

    private static final String SLUG_PARAMETER = "slug";

    private static final String PATH_LOCATION = "path";

    private static final String LIMIT_DESCRIPTION =
            "The page size, at least 1; values above the instance maximum are capped.";

    @Bean
    ErrorCodeSet spaceErrorCodes() {
        List<SpaceError> codes = List.of(SpaceError.values());
        return new ErrorCodeSet(codes);
    }

    @Bean
    GlobalOpenApiCustomizer spaceRoleIsNullable() {
        return SpacesWebConfiguration::markSpaceRoleNullable;
    }

    @Bean
    GlobalOperationCustomizer spacesParameterDocumentation() {
        return (operation, handlerMethod) -> {
            if (isSpacesController(handlerMethod)) {
                documentParameters(operation);
            }
            return operation;
        };
    }

    private static boolean isSpacesController(HandlerMethod handlerMethod) {
        Class<?> beanType = handlerMethod.getBeanType();
        return beanType == SpacesController.class
                || beanType == SpaceMembersController.class
                || beanType == SpaceMemberRolesController.class;
    }

    @Bean
    GlobalOperationCustomizer spacesProblemResponses() {
        return new SpacesProblemResponses();
    }

    private static void documentParameters(Operation operation) {
        List<Parameter> parameters = operation.getParameters();
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                documentParameter(parameter);
            }
        }
    }

    private static void documentParameter(Parameter parameter) {
        String name = parameter.getName();
        if (LIMIT_PARAMETER.equals(name)) {
            documentLimit(parameter);
        }
        if (SLUG_PARAMETER.equals(name) && PATH_LOCATION.equals(parameter.getIn())) {
            documentSlugPattern(parameter);
        }
    }

    private static void documentLimit(Parameter parameter) {
        parameter.setDescription(LIMIT_DESCRIPTION);
        Schema<?> schema = parameter.getSchema();
        if (schema != null) {
            schema.setFormat(null);
        }
    }

    private static void documentSlugPattern(Parameter parameter) {
        Schema<?> schema = parameter.getSchema();
        if (schema != null) {
            schema.setPattern(SpaceSlug.PATTERN);
        }
    }

    private static void markSpaceRoleNullable(OpenAPI openApi) {
        Components components = openApi.getComponents();
        if (components != null) {
            markSpaceRoleNullable(components);
        }
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
}
