package dev.wakey.web;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Encodes and decodes the opaque keyset cursor. The payload carries the sort value and the id, and a
 * malformed cursor is rejected as a 400 {@code invalid_cursor} problem rather than silently ignored.
 */
public final class CursorCodec {

    private static final String FORMAT_VERSION = "v1";

    private static final char SEPARATOR = '\u001f';

    private static final int EXPECTED_PARTS = 3;

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private CursorCodec() {}

    public static String encode(String sortValue, String id) {
        String payload = FORMAT_VERSION + SEPARATOR + sortValue + SEPARATOR + id;
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        return ENCODER.encodeToString(bytes);
    }

    public static CursorPosition decode(String cursor) {
        byte[] bytes;
        try {
            bytes = DECODER.decode(cursor);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(CommonError.INVALID_CURSOR, exception);
        }
        String payload = new String(bytes, StandardCharsets.UTF_8);
        String separator = String.valueOf(SEPARATOR);
        String[] parts = payload.split(separator, -1);
        boolean valid = parts.length == EXPECTED_PARTS && FORMAT_VERSION.equals(parts[0]);
        if (!valid) {
            throw new ApiException(CommonError.INVALID_CURSOR);
        }
        return new CursorPosition(parts[1], parts[2]);
    }

    /** The decoded position of a cursor. */
    public record CursorPosition(String sortValue, String id) {}
}
