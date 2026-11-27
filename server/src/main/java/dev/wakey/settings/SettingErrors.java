package dev.wakey.settings;

import dev.wakey.web.ApiException;
import org.jspecify.annotations.Nullable;

/** Builds the settings validation errors so their codes and pointers stay in one place. */
final class SettingErrors {

    private SettingErrors() {}

    static ApiException invalidValue() {
        return ApiException.invalidField(SettingError.INVALID_VALUE, "/value");
    }

    static ApiException invalidValue(@Nullable Throwable cause) {
        return ApiException.invalidField(SettingError.INVALID_VALUE, "/value", cause);
    }
}
