package dev.wakey.checks;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** The editable fields of a new check before validation. */
public record NewCheck(
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
