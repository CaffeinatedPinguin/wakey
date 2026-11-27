package dev.wakey.web;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Documents the instance page-size bounds on the {@code limit} parameter of every operation. */
final class OpenApiPageLimit {

    private static final String LIMIT_PARAMETER = "limit";

    private OpenApiPageLimit() {}

    static void document(@Nullable Paths paths, int maxPageSize) {
        if (paths != null) {
            for (PathItem pathItem : paths.values()) {
                documentBounds(pathItem.readOperations(), maxPageSize);
            }
        }
    }

    private static void documentBounds(List<Operation> operations, int maxPageSize) {
        for (Operation operation : operations) {
            List<Parameter> parameters = operation.getParameters();
            if (parameters != null) {
                parameters.stream()
                        .filter(OpenApiPageLimit::isLimit)
                        .findFirst()
                        .ifPresent(parameter -> setBounds(parameter, maxPageSize));
            }
        }
    }

    private static void setBounds(Parameter parameter, int maxPageSize) {
        Schema<?> schema = Objects.requireNonNull(parameter.getSchema());
        schema.setMinimum(BigDecimal.ONE);
        schema.setMaximum(BigDecimal.valueOf(maxPageSize));
    }

    private static boolean isLimit(Parameter parameter) {
        return LIMIT_PARAMETER.equals(parameter.getName());
    }
}
