package dev.wakey.spaces;

import dev.wakey.web.ApiException;
import dev.wakey.web.CommonError;
import dev.wakey.web.FieldProblem;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The one space-name rule: after stripping and NFC normalization, 1-100 code points with no Unicode
 * control, format, line or paragraph separator characters and no unpaired surrogates. The HTTP
 * contract documents the same length limit.
 */
public final class SpaceName {

    public static final int MAX_LENGTH = 100;

    private static final Set<Integer> FORBIDDEN_TYPES = Set.of(
            (int) Character.CONTROL,
            (int) Character.FORMAT,
            (int) Character.LINE_SEPARATOR,
            (int) Character.PARAGRAPH_SEPARATOR,
            (int) Character.SURROGATE);

    private SpaceName() {}

    public static String requireValid(String candidate) {
        String normalized = Normalizer.normalize(candidate.strip(), Normalizer.Form.NFC);
        if (isInvalid(normalized)) {
            FieldProblem problem =
                    new FieldProblem("/name", SpaceError.INVALID_NAME.getCode(), Map.of("max", MAX_LENGTH));
            throw new ApiException(CommonError.VALIDATION_FAILED, List.of(problem));
        }
        return normalized;
    }

    private static boolean isInvalid(String value) {
        int length = value.codePointCount(0, value.length());
        return length < 1 || length > MAX_LENGTH || hasForbiddenCodePoint(value);
    }

    private static boolean hasForbiddenCodePoint(String value) {
        return value.codePoints().anyMatch(SpaceName::isForbidden);
    }

    private static boolean isForbidden(int codePoint) {
        return FORBIDDEN_TYPES.contains(Character.getType(codePoint));
    }
}
