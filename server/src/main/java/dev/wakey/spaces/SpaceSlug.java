package dev.wakey.spaces;

import java.util.regex.Pattern;

/** A validated space slug: 3-40 lowercase letters, digits or hyphens, starting with a letter. */
public record SpaceSlug(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[a-z][a-z0-9-]{1,38}[a-z0-9]$");

    public static SpaceSlug parse(String candidate) {
        boolean valid = PATTERN.matcher(candidate).matches();
        if (!valid) {
            throw new InvalidSpaceException(
                    "slug",
                    "invalid_slug",
                    "A space slug must have 3-40 lowercase letters, digits or hyphens, start with a letter and end with a letter or digit");
        }
        return new SpaceSlug(candidate);
    }
}
