package dev.wakey.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Instance-wide API limits; the page size is capped so a client cannot ask for an unbounded list. */
@ConfigurationProperties(prefix = "wakey.api")
public record ApiProperties(@DefaultValue("100") int maxPageSize) {

    public int capPageSize(int requested) {
        return Math.min(requested, maxPageSize);
    }
}
