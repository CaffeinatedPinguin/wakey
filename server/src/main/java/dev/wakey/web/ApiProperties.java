package dev.wakey.web;

import jakarta.validation.constraints.Positive;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Instance-wide API limits; the page size is capped so a client cannot ask for an unbounded list. */
@Validated
@ConfigurationProperties(prefix = "wakey.api")
public record ApiProperties(
        @Positive int defaultPageSize, @Positive int maxPageSize) {

    public ApiProperties {
        if (maxPageSize < 1) {
            throw new IllegalArgumentException("wakey.api.max-page-size must be at least 1");
        }
        if (defaultPageSize < 1) {
            throw new IllegalArgumentException("wakey.api.default-page-size must be at least 1");
        }
        if (defaultPageSize > maxPageSize) {
            throw new IllegalArgumentException("wakey.api.default-page-size must not exceed wakey.api.max-page-size");
        }
    }

    public int pageSize(@Nullable Integer limit) {
        if (limit == null) {
            return defaultPageSize;
        }
        return Math.max(1, Math.min(limit, maxPageSize));
    }
}
