package dev.wakey.spaces;

/**
 * The one space-name rule: after stripping and NFC normalization, 1-100 code points with no Unicode
 * control, format, line or paragraph separator characters and no unpaired surrogates. The HTTP
 * contract documents the same length limit.
 */
public final class SpaceName {

    public static final int MAX_LENGTH = 100;

    private SpaceName() {}
}
