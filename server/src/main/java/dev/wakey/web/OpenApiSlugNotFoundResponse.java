package dev.wakey.web;

import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

/** Documents the single {@code 404} answer of every operation that reads the {@code {slug}} path. */
final class OpenApiSlugNotFoundResponse {

    private static final String NOT_FOUND = "404";

    private static final String SLUG = "{slug}";

    private OpenApiSlugNotFoundResponse() {}

    static OperationCustomizer customizer(String errorSchemaRef) {
        return (operation, handlerMethod) -> {
            if (readsSlug(handlerMethod)) {
                ApiResponses responses = operation.getResponses();
                if (responses == null) {
                    responses = new ApiResponses();
                    operation.setResponses(responses);
                }
                String detail = CommonError.NOT_FOUND.getDetail();
                ApiResponse response = OpenApiErrorResponse.of(errorSchemaRef, detail);
                responses.addApiResponse(NOT_FOUND, response);
            }
            return operation;
        };
    }

    private static boolean readsSlug(HandlerMethod handlerMethod) {
        Class<?> beanType = handlerMethod.getBeanType();
        RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(beanType, RequestMapping.class);
        RequestMapping methodMapping =
                AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequestMapping.class);
        return containsSlug(classMapping) || containsSlug(methodMapping);
    }

    private static boolean containsSlug(@Nullable RequestMapping mapping) {
        return mapping != null && containsSlugPath(mapping.path());
    }

    private static boolean containsSlugPath(String[] paths) {
        for (String path : paths) {
            if (path.contains(SLUG)) {
                return true;
            }
        }
        return false;
    }
}
