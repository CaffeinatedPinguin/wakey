package dev.wakey.api.spaces;

import dev.wakey.security.CurrentUser;
import dev.wakey.spaces.InvalidSpaceException;
import dev.wakey.spaces.SlugTakenException;
import dev.wakey.spaces.SpaceAccessDeniedException;
import dev.wakey.spaces.SpaceNotFoundException;
import dev.wakey.spaces.SpacePrincipal;
import dev.wakey.spaces.SpaceService;
import dev.wakey.spaces.SpaceView;
import dev.wakey.web.ApiProblemException;
import dev.wakey.web.CursorPage;
import dev.wakey.web.Problems;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

/** The adapter that turns the spaces module's domain errors into the HTTP Problem Details contract. */
final class SpacesErrors {

    private SpacesErrors() {}

    static CurrentUser currentUser(Authentication authentication) {
        return CurrentUser.from(authentication);
    }

    static SpacePrincipal principal(Authentication authentication) {
        return principal(currentUser(authentication));
    }

    static SpacePrincipal principal(CurrentUser user) {
        return new SpacePrincipal(user.subject(), user.admin());
    }

    static SpacePrincipal principalFor(Authentication authentication, String slug) {
        CurrentUser user = currentUser(authentication);
        requireTokenSpace(user, slug);
        return principal(user);
    }

    static Optional<String> tokenSpaceSlug(Authentication authentication) {
        CurrentUser user = currentUser(authentication);
        return Optional.ofNullable(user.tokenSpaceSlug());
    }

    static CursorPage<SpaceResponse> tokenSpacePage(
            SpaceService spaces, Authentication authentication, String tokenSpaceSlug) {
        SpacePrincipal actor = principal(authentication);
        SpaceView view = mapped(() -> spaces.view(actor, tokenSpaceSlug));
        SpaceResponse response = SpaceResponse.from(view);
        return new CursorPage<>(List.of(response), null);
    }

    private static void requireTokenSpace(CurrentUser user, String slug) {
        @Nullable String tokenSpaceSlug = user.tokenSpaceSlug();
        if (tokenSpaceSlug != null && !tokenSpaceSlug.equals(slug)) {
            throw new ApiProblemException(HttpStatus.NOT_FOUND, "not_found");
        }
    }

    static <T> T mapped(Supplier<T> action) {
        try {
            return action.get();
        } catch (SpaceNotFoundException exception) {
            throw new ApiProblemException(HttpStatus.NOT_FOUND, "not_found", exception);
        } catch (SpaceAccessDeniedException exception) {
            throw new ApiProblemException(HttpStatus.FORBIDDEN, "forbidden", exception);
        } catch (InvalidSpaceException exception) {
            Map<String, Object> error = Problems.error("/" + exception.field(), exception.code());
            throw new ApiProblemException(HttpStatus.BAD_REQUEST, "validation_failed", List.of(error), exception);
        } catch (SlugTakenException exception) {
            throw new ApiProblemException(HttpStatus.CONFLICT, "slug_taken", exception);
        }
    }
}
