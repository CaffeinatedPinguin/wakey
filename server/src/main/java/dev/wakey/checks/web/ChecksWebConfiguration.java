package dev.wakey.checks.web;

import io.swagger.v3.core.util.RefUtils;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** References the named enum schemas from the checks query parameters, which springdoc keeps inline. */
@Configuration(proxyBeanMethods = false)
class ChecksWebConfiguration {

    private static final Map<String, String> ENUM_SCHEMAS =
            Map.of("state", "CheckState", "type", "CheckType", "pausedReason", "PausedReason");

    @Bean
    GlobalOperationCustomizer enumParameterSchemas() {
        return (operation, handlerMethod) -> {
            if (handlerMethod.getBeanType() == ChecksController.class) {
                referenceEnumSchemas(operation);
            }
            return operation;
        };
    }

    private static void referenceEnumSchemas(Operation operation) {
        List<Parameter> parameters = operation.getParameters();
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                referenceEnumSchema(parameter);
            }
        }
    }

    private static void referenceEnumSchema(Parameter parameter) {
        String schemaName = ENUM_SCHEMAS.get(parameter.getName());
        Schema<?> schema = parameter.getSchema();
        if (schemaName != null && schema != null) {
            schema.setEnum(null);
            schema.set$ref(RefUtils.constructRef(schemaName));
        }
    }
}
