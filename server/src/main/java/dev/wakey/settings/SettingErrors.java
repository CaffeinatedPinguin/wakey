package dev.wakey.settings;

import dev.wakey.web.ApiException;
import dev.wakey.web.ApiFieldError;
import dev.wakey.web.CommonError;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Builds the settings validation errors so their codes and pointers stay in one place. */
final class SettingErrors {

    private SettingErrors() {}

    static ApiException invalidValue() {
        return invalidValue(null);
    }

    static ApiException invalidValue(@Nullable Throwable cause) {
        ApiFieldError error = new ApiFieldError("/value", SettingError.INVALID_VALUE.getCode(), Map.of());
        return new ApiException(CommonError.VALIDATION_FAILED, List.of(error), Map.of(), cause);
    }
}
