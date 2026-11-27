package dev.wakey.spaces.web;

import dev.wakey.spaces.SpaceService;
import dev.wakey.spaces.SpaceView;
import dev.wakey.web.ApiException;
import dev.wakey.web.CommonError;
import dev.wakey.web.CurrentUser;
import dev.wakey.web.TokenSpaces;
import java.security.Principal;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Turns the HTTP authentication into the space actor of one controller argument. It applies the
 * token scope, the space visibility and the write rule before a request body is read.
 */
final class SpaceActors {

    private static final String SLUG_VARIABLE = "slug";

    private static final Set<Class<?>> SUPPORTED_TYPES = Set.of(
            CurrentUser.class,
            SpaceReader.class,
            SpaceWriter.class,
            SpaceUpsert.class,
            SessionUser.class,
            SpaceCaller.class);

    private final SpaceService spaces;

    SpaceActors(SpaceService spaces) {
        this.spaces = spaces;
    }

    boolean supports(Class<?> type) {
        return SUPPORTED_TYPES.contains(type);
    }

    Object resolve(Class<?> type, NativeWebRequest request) {
        Object resolved;
        if (isSpaceType(type)) {
            resolved = resolveSpace(type, request);
        } else {
            resolved = resolveCaller(type, request);
        }
        return resolved;
    }

    private Object resolveSpace(Class<?> type, NativeWebRequest request) {
        Authentication authentication = authentication(request);
        String slug = slug(request);
        TokenSpaces.requireSpace(authentication, slug);
        CurrentUser user = CurrentUser.from(authentication);
        Object argument;
        if (type == SpaceReader.class) {
            argument = new SpaceReader(user, spaces.view(user, slug));
        } else if (type == SpaceWriter.class) {
            SpaceView view = spaces.viewForWrite(user, slug);
            boolean viaToken = TokenSpaces.of(authentication).isPresent();
            argument = new SpaceWriter(user, view, viaToken);
        } else {
            requireUpsertAccess(user, slug);
            argument = new SpaceUpsert(user);
        }
        return argument;
    }

    private Object resolveCaller(Class<?> type, NativeWebRequest request) {
        Object argument;
        if (type == CurrentUser.class) {
            Authentication authentication = authentication(request);
            argument = CurrentUser.from(authentication);
        } else if (type == SpaceCaller.class) {
            argument = spaceCaller(request);
        } else {
            argument = sessionUser(request);
        }
        return argument;
    }

    private void requireUpsertAccess(CurrentUser user, String slug) {
        if (spaces.exists(slug)) {
            spaces.viewForWrite(user, slug);
        }
    }

    private static SpaceCaller spaceCaller(NativeWebRequest request) {
        Authentication authentication = authentication(request);
        String slug = TokenSpaces.of(authentication).orElse("");
        return new SpaceCaller(CurrentUser.from(authentication), slug);
    }

    private static SessionUser sessionUser(NativeWebRequest request) {
        Authentication authentication = authentication(request);
        if (TokenSpaces.of(authentication).isPresent()) {
            throw new ApiException(SessionError.INSUFFICIENT_SCOPE);
        }
        return new SessionUser(CurrentUser.from(authentication));
    }

    private static boolean isSpaceType(Class<?> type) {
        return type == SpaceReader.class || type == SpaceWriter.class || type == SpaceUpsert.class;
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
