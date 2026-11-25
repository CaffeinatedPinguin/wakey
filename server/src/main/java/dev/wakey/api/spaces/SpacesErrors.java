package dev.wakey.api.spaces;

import dev.wakey.security.CurrentUser;
import dev.wakey.spaces.InvalidSpaceException;
import dev.wakey.spaces.SlugTakenException;
import dev.wakey.spaces.SpaceAccessDeniedException;
import dev.wakey.spaces.SpaceNotFoundException;
import dev.wakey.spaces.SpacePrincipal;
import dev.wakey.web.ApiProblemException;
import dev.wakey.web.Problems;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

/** The adapter that turns the spaces module's domain errors into the HTTP Problem Details contract. */
final class SpacesErrors {

    private SpacesErrors() {}

    static SpacePrincipal principal(Authentication authentication) {
        CurrentUser user = CurrentUser.from(authentication);
        return new SpacePrincipal(user.subject(), user.admin());
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
