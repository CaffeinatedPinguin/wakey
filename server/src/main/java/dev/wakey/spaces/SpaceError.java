package dev.wakey.spaces;

import dev.wakey.web.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** The error codes of the spaces module: slugs, versions and member fields. */
@Getter
@RequiredArgsConstructor
public enum SpaceError implements ErrorCode {
    SLUG_TAKEN(HttpStatus.CONFLICT, "A space with that slug already exists."),
    VERSION_CONFLICT(HttpStatus.CONFLICT, "The space changed since the supplied version."),
    INVALID_NAME(
            HttpStatus.BAD_REQUEST,
            "A space name must have 1-"
                    + SpaceName.MAX_LENGTH
                    + " characters without control or bidirectional formatting characters."),
    INVALID_SLUG(
            HttpStatus.BAD_REQUEST,
            "A space slug must have "
                    + SpaceSlug.MIN_LENGTH
                    + "-"
                    + SpaceSlug.MAX_LENGTH
                    + " lowercase letters, digits or hyphens, start with a letter and end with a letter or digit."),
    INVALID_SUBJECT(
            HttpStatus.BAD_REQUEST,
            "A member subject must have 1-"
                    + SpaceService.MAX_SUBJECT_LENGTH
                    + " characters without control or bidirectional formatting characters.");

    private final HttpStatus status;

    private final String detail;
}
