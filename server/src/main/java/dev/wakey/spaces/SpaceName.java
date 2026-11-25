package dev.wakey.spaces;

/**
 * The one space-name rule: after stripping, 1-100 characters with no Unicode control or
 * bidirectional formatting characters. The HTTP contract documents the same length limit.
 */
public final class SpaceName {

    public static final int MAX_LENGTH = 100;

    private SpaceName() {}

    public static String requireValid(String candidate) {
        String stripped = candidate.strip();
        boolean invalid = stripped.isEmpty() || stripped.length() > MAX_LENGTH || containsControlOrFormat(stripped);
        if (invalid) {
            throw new InvalidSpaceException(
                    "name",
                    "invalid_name",
                    "A space name must have 1-100 characters without control or bidirectional formatting characters");
        }
        return stripped;
    }

    private static boolean containsControlOrFormat(String value) {
        return value.codePoints().anyMatch(SpaceName::isControlOrFormat);
    }

    private static boolean isControlOrFormat(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.CONTROL || type == Character.FORMAT;
    }
}
