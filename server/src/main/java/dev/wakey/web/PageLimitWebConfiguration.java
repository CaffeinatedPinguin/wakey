package dev.wakey.web;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the page-size argument resolver and documents the {@code limit} parameter of every
 * operation that takes a {@link PageLimit}, while the resolved type stays hidden from the contract.
 */
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
class PageLimitWebConfiguration implements WebMvcConfigurer {

    private static final String LIMIT_PARAMETER = "limit";

    private static final String LIMIT_DESCRIPTION = "The maximum number of items to return";

    private static final int LIMIT_EXAMPLE = 20;

    private static final String QUERY_LOCATION = "query";

    private static final String PATH_LOCATION = "path";

    private static final String NULL_TYPE = "null";

    static {
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(PageLimit.class);
    }

    private final ApiProperties apiProperties;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new PageLimitArgumentResolver(apiProperties));
    }

    @Bean
    GlobalOperationCustomizer pageLimitParameter() {
        return (operation, handlerMethod) -> {
            if (usesPageLimit(handlerMethod)) {
                documentLimit(operation);
            }
            return operation;
        };
    }

    private static boolean usesPageLimit(HandlerMethod handlerMethod) {
        for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
            if (parameter.getParameterType() == PageLimit.class) {
                return true;
            }
        }
        return false;
    }

    private static void documentLimit(Operation operation) {
        List<Parameter> parameters = operation.getParameters();
        if (parameters == null) {
            parameters = new ArrayList<>();
            operation.setParameters(parameters);
        }
        if (!hasLimit(parameters)) {
            parameters.add(indexOfFirstQuery(parameters), limitParameter());
        }
    }

    private static boolean hasLimit(List<Parameter> parameters) {
        for (Parameter parameter : parameters) {
            if (LIMIT_PARAMETER.equals(parameter.getName())) {
                return true;
            }
        }
        return false;
    }

    private static int indexOfFirstQuery(List<Parameter> parameters) {
        for (int index = 0; index < parameters.size(); index++) {
            String location = parameters.get(index).getIn();
            if (!PATH_LOCATION.equals(location)) {
                return index;
            }
        }
        return parameters.size();
    }

    private static Parameter limitParameter() {
        Parameter parameter = new Parameter();
        parameter.setName(LIMIT_PARAMETER);
        parameter.setIn(QUERY_LOCATION);
        parameter.setDescription(LIMIT_DESCRIPTION);
        parameter.setExample(LIMIT_EXAMPLE);
        parameter.setRequired(false);
        parameter.setSchema(limitSchema());
        return parameter;
    }

    private static IntegerSchema limitSchema() {
        IntegerSchema schema = new IntegerSchema();
        schema.addType(NULL_TYPE);
        schema.setDescription(LIMIT_DESCRIPTION);
        schema.setExample(LIMIT_EXAMPLE);
        schema.setExclusiveMinimumValue(BigDecimal.ZERO);
        return schema;
    }
}
