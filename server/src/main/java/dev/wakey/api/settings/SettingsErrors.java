package dev.wakey.api.settings;

import dev.wakey.security.CurrentUser;
import dev.wakey.settings.InvalidSettingException;
import dev.wakey.settings.SettingOutOfPlanException;
import dev.wakey.settings.UnknownPlanException;
import dev.wakey.spaces.SpaceAccessDeniedException;
import dev.wakey.spaces.SpaceNotFoundException;
import dev.wakey.spaces.SpacePrincipal;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceView;
import dev.wakey.web.ApiProblemException;
import dev.wakey.web.Problems;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

/** The adapter that turns the settings and spaces domain errors into the Problem Details contract. */
final class SettingsErrors {

    private static final String NOT_FOUND = "not_found";

    private static final String FORBIDDEN = "forbidden";

    private SettingsErrors() {}

    static SpacePrincipal principal(Authentication authentication) {
        CurrentUser user = CurrentUser.from(authentication);
        return new SpacePrincipal(user.subject(), user.admin());
    }

    static <T> T mapped(Supplier<T> action) {
        try {
            return action.get();
        } catch (SpaceNotFoundException exception) {
            throw new ApiProblemException(HttpStatus.NOT_FOUND, NOT_FOUND, exception);
        } catch (SpaceAccessDeniedException exception) {
            throw new ApiProblemException(HttpStatus.FORBIDDEN, FORBIDDEN, exception);
        } catch (UnknownPlanException exception) {
            throw new ApiProblemException(HttpStatus.BAD_REQUEST, "unknown_plan", planParams(exception), exception);
        } catch (SettingOutOfPlanException exception) {
            throw new ApiProblemException(HttpStatus.BAD_REQUEST, "out_of_plan", boundParams(exception), exception);
        } catch (InvalidSettingException exception) {
            throw new ApiProblemException(
                    HttpStatus.BAD_REQUEST, "validation_failed", invalidErrors(exception), exception);
        }
    }

    static void run(Runnable action) {
        mapped(() -> {
            action.run();
            return Boolean.TRUE;
        });
    }

    static void requireWriter(SpaceView view) {
        boolean writer = view.admin() || view.role() == SpaceRole.EDITOR;
        if (!writer) {
            throw new SpaceAccessDeniedException("Only an editor or an administrator can change settings");
        }
    }

    static void requireAdmin(SpaceView view) {
        if (!view.admin()) {
            throw new SpaceAccessDeniedException("Only an administrator can assign a plan");
        }
    }

    private static Map<String, Object> planParams(UnknownPlanException exception) {
        return Map.of("plan", exception.planName());
    }

    private static Map<String, Object> boundParams(SettingOutOfPlanException exception) {
        return Map.of(
                "bound", exception.bound(),
                "planName", exception.planName(),
                "key", exception.key());
    }

    private static List<Map<String, Object>> invalidErrors(InvalidSettingException exception) {
        String pointer = exception.value().isEmpty() ? "/key" : "/value";
        return List.of(Problems.error(pointer, "invalid_value"));
    }
}
