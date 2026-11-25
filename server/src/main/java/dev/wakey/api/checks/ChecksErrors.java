package dev.wakey.api.checks;

import dev.wakey.checks.CheckNotFoundException;
import dev.wakey.checks.CheckVersionConflictException;
import dev.wakey.checks.InvalidCheckException;
import dev.wakey.spaces.InvalidSpaceException;
import dev.wakey.spaces.SpaceAccessDeniedException;
import dev.wakey.spaces.SpaceNotFoundException;
import dev.wakey.web.ApiProblemException;
import dev.wakey.web.Problems;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;

/** Turns the checks and spaces domain errors into the shared HTTP Problem Details contract. */
final class ChecksErrors {

    private ChecksErrors() {}

    static <T> T mapped(Supplier<T> action) {
        try {
            return action.get();
        } catch (SpaceNotFoundException | CheckNotFoundException exception) {
            throw new ApiProblemException(HttpStatus.NOT_FOUND, "not_found", exception);
        } catch (SpaceAccessDeniedException exception) {
            throw forbidden(exception);
        } catch (CheckVersionConflictException exception) {
            throw new ApiProblemException(HttpStatus.CONFLICT, "version_conflict", exception);
        } catch (InvalidCheckException exception) {
            throw validation(exception.field(), exception.code(), exception);
        } catch (InvalidSpaceException exception) {
            throw validation(exception.field(), exception.code(), exception);
        }
    }

    static void run(Runnable action) {
        mapped(() -> {
            action.run();
            return Boolean.TRUE;
        });
    }

    static ApiProblemException forbidden() {
        return new ApiProblemException(HttpStatus.FORBIDDEN, "forbidden");
    }

    private static ApiProblemException forbidden(SpaceAccessDeniedException exception) {
        return new ApiProblemException(HttpStatus.FORBIDDEN, "forbidden", exception);
    }

    private static ApiProblemException validation(String field, String code, RuntimeException cause) {
        Map<String, Object> error = Problems.error("/" + field, code);
        return new ApiProblemException(HttpStatus.BAD_REQUEST, "validation_failed", List.of(error), cause);
    }
}
