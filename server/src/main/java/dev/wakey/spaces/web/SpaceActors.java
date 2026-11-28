package dev.wakey.spaces.web;

import dev.wakey.spaces.SpaceService;
import dev.wakey.web.ApiException;
import dev.wakey.web.CommonError;
import dev.wakey.web.CurrentUser;
import dev.wakey.web.TokenSpaces;
import java.security.Principal;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Resolves the space actor arguments of the HTTP adapters from the request authentication. It
 * applies the token scope, the space visibility and the write rule before a request body is read.
 */
@Slf4j
@Component
@RequiredArgsConstructor
final class SpaceActors implements HandlerMethodArgumentResolver {

    private static final String SLUG_VARIABLE = "slug";

    private final Map<Class<?>, Function<NativeWebRequest, Object>> resolvers = Map.of(
            CurrentUser.class, SpaceActors::currentUser,
            SpaceReader.class, this::spaceReader,
            SpaceWriter.class, this::spaceWriter,
            SpaceUpsert.class, this::spaceUpsert,
            SessionUser.class, SpaceActors::sessionUser,
            SpaceCaller.class, SpaceActors::spaceCaller);

    private final SpaceService spaces;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return resolvers.containsKey(parameter.getParameterType());
    }

    @Override
    public @Nullable Object resolveArgument(
            MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            NativeWebRequest request,
            @Nullable WebDataBinderFactory binderFactory) {
        Class<?> type = parameter.getParameterType();
        Function<NativeWebRequest, Object> resolver = resolvers.get(type);
        if (resolver == null) {
            throw new IllegalStateException("Unsupported space actor type " + type.getName());
        }
        return resolver.apply(request);
    }

    private Object spaceReader(NativeWebRequest request) {
        String slug = slug(request);
        CurrentUser user = spaceUser(request, slug);
        return new SpaceReader(user, spaces.view(user, slug));
    }

    private Object spaceWriter(NativeWebRequest request) {
        String slug = slug(request);
        CurrentUser user = spaceUser(request, slug);
        boolean viaToken = TokenSpaces.of(authentication(request)).isPresent();
        return new SpaceWriter(user, spaces.viewForWrite(user, slug), viaToken);
    }

    private Object spaceUpsert(NativeWebRequest request) {
        String slug = slug(request);
        CurrentUser user = spaceUser(request, slug);
        if (spaces.exists(slug)) {
            spaces.viewForWrite(user, slug);
        }
        return new SpaceUpsert(user);
    }

    private CurrentUser spaceUser(NativeWebRequest request, String slug) {
        Authentication authentication = authentication(request);
        TokenSpaces.requireSpace(authentication, slug);
        return CurrentUser.from(authentication);
    }

    private static Object currentUser(NativeWebRequest request) {
        return CurrentUser.from(authentication(request));
    }

    private static Object spaceCaller(NativeWebRequest request) {
        Authentication authentication = authentication(request);
        String slug = TokenSpaces.of(authentication).orElse("");
        return new SpaceCaller(CurrentUser.from(authentication), slug);
    }

    private static Object sessionUser(NativeWebRequest request) {
        Authentication authentication = authentication(request);
        if (TokenSpaces.of(authentication).isPresent()) {
            log.debug("Rejected a session-only argument for an API token request");
            throw new ApiException(CommonError.INSUFFICIENT_SCOPE);
        }
        return new SessionUser(CurrentUser.from(authentication));
    }

    private static Authentication authentication(NativeWebRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal instanceof Authentication) {
            return (Authentication) principal;
        }
        throw new ApiException(CommonError.UNAUTHENTICATED);
    }

    private static String slug(NativeWebRequest request) {
        Object variables =
                request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        String slug = "";
        if (variables instanceof Map<?, ?> map) {
            Object value = map.get(SLUG_VARIABLE);
            if (value instanceof String found) {
                slug = found;
            }
        }
        return slug;
    }
}
