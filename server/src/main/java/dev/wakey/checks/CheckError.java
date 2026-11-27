package dev.wakey.checks;

import dev.wakey.web.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** The error codes of the checks module: plan limits and field rules; shared codes live in their one home. */
@Getter
@RequiredArgsConstructor
public enum CheckError implements ErrorCode {
    PLAN_LIMIT(HttpStatus.BAD_REQUEST, "The space reached the check limit of its plan."),
    INVALID_TARGET(HttpStatus.BAD_REQUEST, "The check target is invalid."),
    INVALID_TIMEOUT(HttpStatus.BAD_REQUEST, "The check timeout is invalid."),
    INVALID_TAGS(HttpStatus.BAD_REQUEST, "The check tags are invalid."),
    INVALID_RUNBOOK_URL(HttpStatus.BAD_REQUEST, "The runbook URL is invalid."),
    INVALID_GROUP_NAME(HttpStatus.BAD_REQUEST, "The check group name is invalid."),
    INVALID_DESCRIPTION(HttpStatus.BAD_REQUEST, "The check description is invalid."),
    LOCATION_ERROR_CANNOT_BE_DOWN(HttpStatus.BAD_REQUEST, "A location network error cannot make the target down."),
    BULK_COUNT_CHANGED(HttpStatus.CONFLICT, "The number of matching checks changed since the dialog was shown.");

    private final HttpStatus status;

    private final String detail;
}
