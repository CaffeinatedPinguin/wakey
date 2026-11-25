package dev.wakey.web;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Instance-wide API limits; the page size is capped so a client cannot ask for an unbounded list. */
@ConfigurationProperties(prefix = "wakey.api")
public record ApiProperties(
        @DefaultValue("20") int defaultPageSize,
        @DefaultValue("100") int maxPageSize) {

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
        return limit == null ? defaultPageSize : Math.min(limit, maxPageSize);
    }
}
