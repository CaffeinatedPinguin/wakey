package dev.wakey.tokens;

import dev.wakey.web.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** The error codes of the tokens module: limits, rejected authentication and expiry; names use the shared code. */
@Getter
@RequiredArgsConstructor
public enum TokenError implements ErrorCode {
    TOKEN_LIMIT(HttpStatus.BAD_REQUEST, "The owner has reached the token limit of the space."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "The API token is invalid."),
    INVALID_EXPIRY(HttpStatus.BAD_REQUEST, "A token expiry must be in the future.");

    private final HttpStatus status;

    private final String detail;
}
