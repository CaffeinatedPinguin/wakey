package dev.wakey.api.spaces;

import dev.wakey.spaces.SpaceError;
import dev.wakey.web.ApiException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;

/** Parses the conditional request headers of the space put operation. */
final class SpacesPreconditions {

    private static final String IF_NONE_MATCH = "If-None-Match";

    private static final String IF_MATCH = "If-Match";

    private static final String IF_NONE_MATCH_ANY = "*";

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
        throw new ApiException(SpaceError.INVALID_PRECONDITION);
    }

    static @Nullable Long version(HttpHeaders headers) {
        String value = headers.getFirst(IF_MATCH);
        if (value == null) {
            return null;
        }
        Matcher matcher = IF_MATCH_VERSION.matcher(value.strip());
        if (!matcher.matches()) {
            throw new ApiException(SpaceError.INVALID_PRECONDITION);
        }
        try {
            return Long.valueOf(matcher.group(1));
        } catch (NumberFormatException exception) {
            throw new ApiException(SpaceError.INVALID_PRECONDITION, exception);
        }
    }
}
