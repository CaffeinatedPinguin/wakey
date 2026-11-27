package dev.wakey.web;

import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves the page size of a list request from the {@code limit} query parameter, applying the
 * default and the instance maximum.
 */
final class PageLimitArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String LIMIT_PARAMETER = "limit";

    private static final String LIMIT_POINTER = "/" + LIMIT_PARAMETER;

    private final ApiProperties apiProperties;

    PageLimitArgumentResolver(ApiProperties apiProperties) {
        this.apiProperties = apiProperties;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == PageLimit.class;
    }

    @Override
    public @Nullable Object resolveArgument(
            MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            NativeWebRequest request,
            @Nullable WebDataBinderFactory binderFactory) {
        String raw = request.getParameter(LIMIT_PARAMETER);
        Integer limit = parse(raw);
        return new PageLimit(apiProperties.pageSize(limit));
    }

    private static @Nullable Integer parse(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        int limit = parseInteger(raw);
        if (limit < 1) {
            throw ApiException.withField(CommonError.VALIDATION_FAILED, LIMIT_POINTER);
        }
        return limit;
    }

    private static int parseInteger(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            throw new ApiException(CommonError.BAD_REQUEST, exception);
        }
    }
}
