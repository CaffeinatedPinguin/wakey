package dev.wakey.settings;

import dev.wakey.web.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** The error codes of the settings module: unknown plans, plan bounds and field rules. */
@Getter
@RequiredArgsConstructor
public enum SettingError implements ErrorCode {
    OUT_OF_PLAN(HttpStatus.BAD_REQUEST, "A setting is outside the bounds of the space plan."),
    UNKNOWN_PLAN(HttpStatus.BAD_REQUEST, "The requested plan is not configured on this instance."),
    INVALID_KEY(HttpStatus.BAD_REQUEST, "The setting key is unknown."),
    INVALID_VALUE(HttpStatus.BAD_REQUEST, "The setting value is invalid.");

    private final HttpStatus status;

    private final String detail;
}
