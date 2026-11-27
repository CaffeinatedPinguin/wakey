package dev.wakey.spaces;

import java.util.regex.Pattern;

/**
 * The one slug rule: 3-40 lowercase letters, digits or hyphens, starting with a letter and ending with a
 * letter or digit. The pattern is exposed so the HTTP contract documents the same rule.
 */
public final class SpaceSlug {

    public static final int MIN_LENGTH = 3;

    public static final int MAX_LENGTH = 40;

    public static final String PATTERN = "^[a-z][a-z0-9-]{" + (MIN_LENGTH - 2) + "," + (MAX_LENGTH - 2) + "}[a-z0-9]$";

    private static final Pattern COMPILED = Pattern.compile(PATTERN);

    private SpaceSlug() {}

    public static boolean isValid(String candidate) {
        return COMPILED.matcher(candidate).matches();
    }
}
