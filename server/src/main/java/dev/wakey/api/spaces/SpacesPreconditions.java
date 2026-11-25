package dev.wakey.api.spaces;

import dev.wakey.web.ApiProblemException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/** Parses the conditional request headers of the space put operation. */
final class SpacesPreconditions {

    private static final String IF_NONE_MATCH = "If-None-Match";

    private static final String IF_MATCH = "If-Match";

    private static final String IF_NONE_MATCH_ANY = "*";

    private static final String INVALID_PRECONDITION = "invalid_precondition";

    private static final Pattern IF_MATCH_VERSION = Pattern.compile("\"([0-9]+)\"");

    private SpacesPreconditions() {}

    static boolean isCreateOnly(HttpHeaders headers) {
        String value = headers.getFirst(IF_NONE_MATCH);
        if (value == null) {
            return false;
        }
        if (IF_NONE_MATCH_ANY.equals(value.strip())) {
            return true;
        }
        throw new ApiProblemException(
                HttpStatus.BAD_REQUEST, INVALID_PRECONDITION, "The If-None-Match header only accepts *.");
    }

    static @Nullable Long version(HttpHeaders headers) {
        String value = headers.getFirst(IF_MATCH);
        if (value == null) {
            return null;
        }
        Matcher matcher = IF_MATCH_VERSION.matcher(value.strip());
        if (!matcher.matches()) {
            throw new ApiProblemException(
                    HttpStatus.BAD_REQUEST, INVALID_PRECONDITION, "The If-Match header must contain a quoted version.");
        }
        try {
            return Long.valueOf(matcher.group(1));
        } catch (NumberFormatException exception) {
            throw new ApiProblemException(
                    HttpStatus.BAD_REQUEST, INVALID_PRECONDITION, "The If-Match version is out of range.", exception);
        }
    }
}
