package dev.wakey.web;

import java.util.Locale;
import org.springframework.http.HttpStatus;

/** One closed set of error codes: the wire code, its HTTP status and its user-facing detail. */
public interface ErrorCode {

    String name();

    HttpStatus getStatus();

    String getDetail();

    default String getCode() {
        return name().toLowerCase(Locale.ROOT);
    }
}
