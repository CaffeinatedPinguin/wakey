package dev.wakey.spaces;

import dev.wakey.web.ApiException;
import dev.wakey.web.CommonError;
import dev.wakey.web.FieldProblem;
import java.util.List;
import java.util.Map;
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

    public static String requireValid(String candidate) {
        boolean valid = COMPILED.matcher(candidate).matches();
        if (!valid) {
            FieldProblem problem = new FieldProblem("/slug", SpaceError.INVALID_SLUG.getCode(), Map.of());
            throw new ApiException(CommonError.VALIDATION_FAILED, List.of(problem));
        }
        return candidate;
    }
}
