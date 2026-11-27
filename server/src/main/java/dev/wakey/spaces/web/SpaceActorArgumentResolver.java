package dev.wakey.spaces.web;

import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Resolves the space actor arguments of the HTTP adapters from the request authentication. */
final class SpaceActorArgumentResolver implements HandlerMethodArgumentResolver {

    private final SpaceActors actors;

    SpaceActorArgumentResolver(SpaceActors actors) {
        this.actors = actors;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return actors.supports(parameter.getParameterType());
    }

    @Override
    public @Nullable Object resolveArgument(
            MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            NativeWebRequest request,
            @Nullable WebDataBinderFactory binderFactory) {
        return actors.resolve(parameter.getParameterType(), request);
    }
}
