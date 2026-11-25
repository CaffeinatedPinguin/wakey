package dev.wakey.spaces;

import dev.wakey.web.ApiException;
import dev.wakey.web.CommonError;
import dev.wakey.web.FieldProblem;
import java.util.List;
import java.util.Map;

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
            FieldProblem problem = new FieldProblem("/name", SpaceError.INVALID_NAME.getCode(), Map.of());
            throw new ApiException(CommonError.VALIDATION_FAILED, List.of(problem));
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
