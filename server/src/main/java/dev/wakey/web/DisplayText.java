package dev.wakey.web;

import java.text.Normalizer;
import java.util.Set;

/**
 * The one rule for user-facing display text: strip the surrounding Unicode whitespace and apply
 * NFC, then count code points and reject Unicode control, format, line and paragraph separator
 * characters and unpaired surrogates. A multi-line value additionally allows a tab and a line feed,
 * which is what the check description needs; every other field is single-line and rejects them like
 * any other control character.
 */
public final class DisplayText {

    private static final Set<Integer> FORBIDDEN_TYPES = Set.of(
            (int) Character.CONTROL,
            (int) Character.FORMAT,
            (int) Character.LINE_SEPARATOR,
            (int) Character.PARAGRAPH_SEPARATOR,
            (int) Character.SURROGATE);

    private static final int TAB = '\t';

    private static final int LINE_FEED = '\n';

    private DisplayText() {}

    public static String normalize(String candidate) {
        return Normalizer.normalize(candidate.strip(), Normalizer.Form.NFC);
    }

    public static boolean isValid(String value, int maxLength, boolean multiline) {
        int length = value.codePointCount(0, value.length());
        boolean withinLength = length >= 1 && length <= maxLength;
        boolean allowed = value.codePoints().noneMatch(codePoint -> isForbidden(codePoint, multiline));
        return withinLength && allowed;
    }

    /** Says whether a single-line value carries a forbidden control, separator or surrogate code point. */
    public static boolean hasForbiddenCodePoint(String value) {
        return value.codePoints().anyMatch(codePoint -> isForbidden(codePoint, false));
    }

    private static boolean isForbidden(int codePoint, boolean multiline) {
        boolean allowedBreak = multiline && (codePoint == TAB || codePoint == LINE_FEED);
        return !allowedBreak && FORBIDDEN_TYPES.contains(Character.getType(codePoint));
    }
}
