package dev.wakey.web;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Encodes and decodes the opaque keyset cursor. The payload carries only the sort value; an oversized or
 * malformed cursor is rejected with a 400 {@code invalid_cursor} error rather than silently ignored.
 */
public final class CursorCodec {

    private static final String FORMAT_VERSION = "v1";

    private static final char SEPARATOR = '\u001f';

    private static final int EXPECTED_PARTS = 2;

    private static final int MAX_UTF8_BYTES_PER_CODE_UNIT = 3;

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private CursorCodec() {}

    public static String encode(String sortValue) {
        String payload = FORMAT_VERSION + SEPARATOR + sortValue;
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        return ENCODER.encodeToString(bytes);
    }

    /**
     * The longest cursor for a sort value of at most {@code maxSortValueLength} UTF-16 code units.
     * One code unit takes at most three UTF-8 bytes; the version and the separator add three bytes.
     */
    public static int maxCursorLength(int maxSortValueLength) {
        int payloadLength = FORMAT_VERSION.length() + 1 + maxSortValueLength * MAX_UTF8_BYTES_PER_CODE_UNIT;
        return (payloadLength * 4 + 2) / 3;
    }

    public static String decode(String cursor, int maxCursorLength) {
        if (cursor.length() > maxCursorLength) {
            throw new ApiException(CommonError.INVALID_CURSOR);
        }
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
        return parts[1];
    }
}
