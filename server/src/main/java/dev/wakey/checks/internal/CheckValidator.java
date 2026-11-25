package dev.wakey.checks.internal;

import dev.wakey.checks.CheckChanges;
import dev.wakey.checks.CheckType;
import dev.wakey.checks.InvalidCheckException;
import dev.wakey.checks.NewCheck;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** Validates and normalizes the editable check fields before they are stored. */
final class CheckValidator {

    private static final int MAX_NAME_LENGTH = 100;

    private static final int MAX_TARGET_LENGTH = 2048;

    private static final int MAX_GROUP_LENGTH = 100;

    private static final int MAX_DESCRIPTION_LENGTH = 1000;

    private static final int MAX_RUNBOOK_LENGTH = 2048;

    private static final int MAX_TAGS = 20;

    private static final int MAX_TAG_LENGTH = 50;

    private static final int MAX_RETRIES = 10;

    private CheckValidator() {}

    static ValidatedCheck validate(NewCheck check) {
        String name = check.name();
        CheckType type = check.type();
        String target = check.target();
        int intervalSeconds = check.intervalSeconds();
        int timeoutSeconds = check.timeoutSeconds();
        int retries = check.retries();
        @Nullable String groupName = check.groupName();
        @Nullable String description = check.description();
        @Nullable String runbookUrl = check.runbookUrl();
        List<String> tags = check.tags();
        return validateDraft(new ValidatedCheck(
                name,
                type,
                target,
                intervalSeconds,
                timeoutSeconds,
                retries,
                groupName,
                description,
                runbookUrl,
                tags));
    }

    static ValidatedCheck validate(CheckChanges changes) {
        String name = changes.name();
        CheckType type = changes.type();
        String target = changes.target();
        int intervalSeconds = changes.intervalSeconds();
        int timeoutSeconds = changes.timeoutSeconds();
        int retries = changes.retries();
        @Nullable String groupName = changes.groupName();
        @Nullable String description = changes.description();
        @Nullable String runbookUrl = changes.runbookUrl();
        List<String> tags = changes.tags();
        return validateDraft(new ValidatedCheck(
                name,
                type,
                target,
                intervalSeconds,
                timeoutSeconds,
                retries,
                groupName,
                description,
                runbookUrl,
                tags));
    }

    private static ValidatedCheck validateDraft(ValidatedCheck draft) {
        String name = requireName(draft.name());
        String target = requireTarget(draft.target());
        int intervalSeconds = draft.intervalSeconds();
        int timeoutSeconds = draft.timeoutSeconds();
        int retries = draft.retries();
        requireInterval(intervalSeconds);
        requireTimeout(timeoutSeconds, intervalSeconds);
        requireRetries(retries);
        List<String> tags = normalizeTags(draft.tags());
        @Nullable String groupName = optionalText(draft.groupName(), MAX_GROUP_LENGTH, "groupName");
        @Nullable String description = optionalText(draft.description(), MAX_DESCRIPTION_LENGTH, "description");
        @Nullable String runbookUrl = optionalText(draft.runbookUrl(), MAX_RUNBOOK_LENGTH, "runbookUrl");
        return new ValidatedCheck(
                name,
                draft.type(),
                target,
                intervalSeconds,
                timeoutSeconds,
                retries,
                groupName,
                description,
                runbookUrl,
                tags);
    }

    private static String requireName(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH) {
            throw new InvalidCheckException("name", "invalid_name", "A check name must have 1-100 characters");
        }
        return trimmed;
    }

    private static String requireTarget(String target) {
        String trimmed = target.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_TARGET_LENGTH) {
            throw new InvalidCheckException("target", "invalid_target", "A target must have 1-2048 characters");
        }
        return trimmed;
    }

    private static void requireInterval(int intervalSeconds) {
        if (intervalSeconds < 1) {
            throw new InvalidCheckException(
                    "intervalSeconds", "invalid_interval", "An interval must be at least 1 second");
        }
    }

    private static void requireTimeout(int timeoutSeconds, int intervalSeconds) {
        boolean valid = timeoutSeconds >= 1 && timeoutSeconds <= intervalSeconds;
        if (!valid) {
            throw new InvalidCheckException(
                    "timeoutSeconds", "invalid_timeout", "A timeout must be between 1 second and the interval");
        }
    }

    private static void requireRetries(int retries) {
        if (retries < 0 || retries > MAX_RETRIES) {
            throw new InvalidCheckException("retries", "invalid_retries", "Retries must be between 0 and 10");
        }
    }

    private static List<String> normalizeTags(List<String> tags) {
        if (tags.size() > MAX_TAGS) {
            throw new InvalidCheckException("tags", "invalid_tags", "A check may have at most 20 tags");
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String tag : tags) {
            String trimmed = tag.trim();
            if (trimmed.isEmpty() || trimmed.length() > MAX_TAG_LENGTH) {
                throw new InvalidCheckException("tags", "invalid_tags", "A tag must have 1-50 characters");
            }
            unique.add(trimmed);
        }
        return List.copyOf(unique);
    }

    private static @Nullable String optionalText(@Nullable String value, int maxLength, String field) {
        @Nullable String trimmed = value == null ? null : value.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            throw new InvalidCheckException(
                    field, "invalid_" + field, "The value must have at most " + maxLength + " characters");
        }
        return trimmed;
    }
}
