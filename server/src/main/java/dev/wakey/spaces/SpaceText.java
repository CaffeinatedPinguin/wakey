package dev.wakey.spaces;

import java.util.Set;

/**
 * The shared Unicode rule for the text fields of a space: no control, format, line or paragraph
 * separator characters and no unpaired surrogates.
 */
final class SpaceText {

    private static final Set<Integer> FORBIDDEN_TYPES = Set.of(
            (int) Character.CONTROL,
            (int) Character.FORMAT,
            (int) Character.LINE_SEPARATOR,
            (int) Character.PARAGRAPH_SEPARATOR,
            (int) Character.SURROGATE);

    private SpaceText() {}

    static boolean hasForbiddenCodePoint(String value) {
        return value.codePoints().anyMatch(SpaceText::isForbidden);
    }

    private static boolean isForbidden(int codePoint) {
        return FORBIDDEN_TYPES.contains(Character.getType(codePoint));
    }
}
