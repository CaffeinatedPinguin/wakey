package dev.wakey.checks;

import org.jspecify.annotations.Nullable;

/** Server-side list filters; a null field does not narrow the result. */
public record CheckFilter(
        @Nullable String q,
        @Nullable CheckState state,
        @Nullable CheckType type,
        @Nullable String tag,
        @Nullable String group,
        @Nullable PausedReason pausedReason) {

    /** A filter that narrows nothing. */
    public static CheckFilter all() {
        return new CheckFilter(null, null, null, null, null, null);
    }
}
