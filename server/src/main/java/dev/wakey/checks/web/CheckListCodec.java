package dev.wakey.checks.web;

import dev.wakey.checks.Check;
import dev.wakey.checks.CheckFieldLimits;
import dev.wakey.checks.CheckPosition;
import dev.wakey.web.ApiException;
import dev.wakey.web.CommonError;
import dev.wakey.web.CursorCodec;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Codes the check list cursor with the generic cursor codec of the web layer. */
final class CheckListCodec {

    private static final char SEPARATOR = '\u0000';

    private static final int UUID_LENGTH = 36;

    private static final int MAX_SORT_VALUE_LENGTH = CheckFieldLimits.MAX_NAME_LENGTH + 1 + UUID_LENGTH;

    private static final int MAX_CURSOR_LENGTH = CursorCodec.maxCursorLength(MAX_SORT_VALUE_LENGTH);

    private CheckListCodec() {}

    /** Decodes the opaque cursor of the check list, rejecting a malformed one as an invalid cursor. */
    static @Nullable CheckPosition decode(@Nullable String encoded) {
        if (encoded == null) {
            return null;
        }
        String sortValue = CursorCodec.decode(encoded, MAX_CURSOR_LENGTH);
        int separator = sortValue.indexOf(SEPARATOR);
        if (separator < 0) {
            throw new ApiException(CommonError.INVALID_CURSOR);
        }
        String name = sortValue.substring(0, separator);
        UUID id = cursorId(sortValue.substring(separator + 1));
        return new CheckPosition(name, id);
    }

    /** The sort value of the check: its name and then its id. */
    static String sortValue(Check check) {
        return check.name() + SEPARATOR + check.id();
    }

    private static UUID cursorId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(CommonError.INVALID_CURSOR, exception);
        }
    }
}
