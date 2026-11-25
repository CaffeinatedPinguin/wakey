package dev.wakey.api.checks;

import dev.wakey.checks.CheckCursor;
import dev.wakey.web.ApiProblemException;
import dev.wakey.web.CursorCodec;
import dev.wakey.web.CursorCodec.CursorPosition;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;

/** Encodes and decodes the checks keyset cursor, rejecting a malformed one as an invalid cursor. */
final class ChecksCursor {

    private ChecksCursor() {}

    static @Nullable CheckCursor decode(@Nullable String encoded) {
        if (encoded == null) {
            return null;
        }
        CursorPosition position = CursorCodec.decode(encoded);
        UUID id = cursorId(position.id());
        return new CheckCursor(position.sortValue(), id);
    }

    static @Nullable String encode(@Nullable CheckCursor cursor) {
        if (cursor == null) {
            return null;
        }
        String id = cursor.id().toString();
        return CursorCodec.encode(cursor.name(), id);
    }

    private static UUID cursorId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new ApiProblemException(HttpStatus.BAD_REQUEST, "invalid_cursor", Map.of(), exception);
        }
    }
}
