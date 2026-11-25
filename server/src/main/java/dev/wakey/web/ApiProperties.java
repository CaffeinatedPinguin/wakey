package dev.wakey.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Instance-wide API limits; the page size is capped so a client cannot ask for an unbounded list. */
@ConfigurationProperties(prefix = "wakey.api")
public record ApiProperties(@DefaultValue("100") int maxPageSize) {

    public ApiProperties {
        if (maxPageSize < 1) {
            throw new IllegalArgumentException("wakey.api.max-page-size must be at least 1");
        }
    }

    public int capPageSize(int requested) {
        if (requested < 1) {
            throw new ApiException(CommonError.VALIDATION_FAILED);
        }
        return Math.min(requested, maxPageSize);
    }
}
