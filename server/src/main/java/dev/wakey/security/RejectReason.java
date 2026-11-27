package dev.wakey.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** The closed set of fixed reasons a rejected request is logged with. */
@Getter
@RequiredArgsConstructor
enum RejectReason {
    ACCESS_DENIED("access-denied"),
    CSRF_TOKEN_MISSING("csrf-token-missing"),
    CSRF_TOKEN_INVALID("csrf-token-invalid"),
    MISSING_AUTHENTICATION("missing-authentication");

    private final String wireName;
}
