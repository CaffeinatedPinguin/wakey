package dev.wakey.checks.internal;

import dev.wakey.checks.CheckType;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Editable check fields after validation, with trimmed text and deduplicated tags. */
record ValidatedCheck(
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
