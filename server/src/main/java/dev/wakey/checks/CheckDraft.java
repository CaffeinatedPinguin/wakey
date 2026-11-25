package dev.wakey.checks;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The editable fields of a check, shared by create and update. The validator normalizes it, so the
 * same record carries the values before and after validation.
 */
public record CheckDraft(
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
