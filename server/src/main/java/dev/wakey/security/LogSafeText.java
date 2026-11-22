package dev.wakey.security;

import java.util.Locale;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

/** Turns client-controlled text into one bounded log line with visible control-character escapes. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class LogSafeText {

    private static final int MAX_LENGTH = 200;

    private static final int HEX_RADIX = 16;

    private static final int PERCENT_ESCAPE_LENGTH = 3;

    static String sanitize(@Nullable String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sanitized = new StringBuilder();
        int index = 0;
        while (index < value.length() && sanitized.length() < MAX_LENGTH) {
            char character = value.charAt(index);
            int encodedControl = percentEncodedControl(value, index);
            if (encodedControl >= 0) {
                appendEscaped(sanitized, (char) encodedControl);
                index += PERCENT_ESCAPE_LENGTH;
            } else if (Character.isISOControl(character)) {
                appendEscaped(sanitized, character);
                index++;
            } else {
                sanitized.append(character);
                index++;
            }
        }
        return truncate(sanitized.toString());
    }

    private static String truncate(String value) {
        String truncated = value;
        if (value.length() > MAX_LENGTH) {
            truncated = value.substring(0, MAX_LENGTH);
        }
        return truncated;
    }

    private static int percentEncodedControl(String value, int index) {
        int digitsStart = index + 1;
        boolean completeEscape = value.charAt(index) == '%' && digitsStart + 2 <= value.length();
        int control = -1;
        if (completeEscape) {
            int high = decodeHexDigit(value.charAt(digitsStart));
            int low = decodeHexDigit(value.charAt(digitsStart + 1));
            boolean validEscape = high >= 0 && low >= 0;
            char decoded = (char) (high * HEX_RADIX + low);
            if (validEscape && Character.isISOControl(decoded)) {
                control = decoded;
            }
        }
        return control;
    }

    private static int decodeHexDigit(char digit) {
        int value = -1;
        if (digit >= '0' && digit <= '9') {
            value = digit - '0';
        } else if (digit >= 'a' && digit <= 'f') {
            value = digit - 'a' + 10;
        } else if (digit >= 'A' && digit <= 'F') {
            value = digit - 'A' + 10;
        }
        return value;
    }

    private static void appendEscaped(StringBuilder sanitized, char character) {
        switch (character) {
            case '\r' -> sanitized.append("\\r");
            case '\n' -> sanitized.append("\\n");
            case '\t' -> sanitized.append("\\t");
            case '\b' -> sanitized.append("\\b");
            case '\f' -> sanitized.append("\\f");
            default -> sanitized.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
        }
    }
}
