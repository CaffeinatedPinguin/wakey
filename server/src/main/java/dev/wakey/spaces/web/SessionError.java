package dev.wakey.spaces.web;

import dev.wakey.web.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * The error of a session-only operation; it keeps the tokens module's wire code and status for a
 * token caller without making the spaces module depend on tokens.
 */
@Getter
@RequiredArgsConstructor
enum SessionError implements ErrorCode {
    INSUFFICIENT_SCOPE(HttpStatus.FORBIDDEN, "The API token does not allow this request.");

    private final HttpStatus status;

    private final String detail;
}
