package dev.wakey.spaces;

import java.text.Normalizer;

/**
 * The one space-name rule: after stripping and NFC normalization, 1-100 code points with no Unicode
 * control, format, line or paragraph separator characters and no unpaired surrogates. The HTTP
 * contract documents the same length limit.
 */
public final class SpaceName {

    public static final int MAX_LENGTH = 100;

    private SpaceName() {}

    /** Strips the surrounding whitespace and applies Unicode NFC, keeping the normalization in one place. */
    public static String normalize(String candidate) {
        return Normalizer.normalize(candidate.strip(), Normalizer.Form.NFC);
    }
}
