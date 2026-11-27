package dev.wakey.checks;

import dev.wakey.web.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** The error codes of the check settings; the wire code stays {@code timeout_exceeds_interval}. */
@Getter
@RequiredArgsConstructor
public enum CheckSettingError implements ErrorCode {
    TIMEOUT_EXCEEDS_INTERVAL(HttpStatus.BAD_REQUEST, "The timeout must not exceed the interval.");

    private final HttpStatus status;

    private final String detail;
}
