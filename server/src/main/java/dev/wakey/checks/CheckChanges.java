package dev.wakey.checks;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** The replacement values of an update; every field is written, so the caller sends the full check. */
public record CheckChanges(
        String name,
        CheckType type,
        String target,
        int intervalSeconds,
        int timeoutSeconds,
        int retries,
        @Nullable String groupName,
        @Nullable String description,
        @Nullable String runbookUrl,
        List<String> tags) {}
