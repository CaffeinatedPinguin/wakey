package dev.wakey.spaces;

import java.util.regex.Pattern;

/**
 * The one slug rule: 3-40 lowercase letters, digits or hyphens, starting with a letter and ending with a
 * letter or digit. The pattern is exposed so the HTTP contract documents the same rule.
 */
public final class SpaceSlug {

    public static final String PATTERN = "^[a-z][a-z0-9-]{1,38}[a-z0-9]$";

    private static final Pattern COMPILED = Pattern.compile(PATTERN);

    private SpaceSlug() {}

    public static String requireValid(String candidate) {
        boolean valid = COMPILED.matcher(candidate).matches();
        if (!valid) {
            throw new InvalidSpaceException(
                    "slug",
                    "invalid_slug",
                    "A space slug must have 3-40 lowercase letters, digits or hyphens, start with a letter and end with a letter or digit");
        }
        return candidate;
    }
}
