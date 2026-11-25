package dev.wakey.checks.internal;

import dev.wakey.checks.CheckDraft;
import dev.wakey.checks.CheckLimits;
import dev.wakey.checks.CheckOutOfPlanException;
import dev.wakey.checks.CheckType;
import dev.wakey.checks.InvalidCheckException;
import dev.wakey.checks.PlanLimits;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
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

    private static final int MIN_PORT = 1;

    private static final int MAX_PORT = 65535;

    private static final Pattern PORT_PATTERN = Pattern.compile("\\d{1,5}");

    private CheckValidator() {}

    static CheckDraft validate(CheckDraft draft, PlanLimits limits) {
        String name = requireName(draft.name());
        String target = requireTarget(draft.target(), draft.type());
        int intervalSeconds = draft.intervalSeconds();
        int timeoutSeconds = draft.timeoutSeconds();
        int retries = draft.retries();
        requireInterval(intervalSeconds);
        requireTimeout(timeoutSeconds, intervalSeconds);
        requireRetries(retries);
        requireWithinPlan(limits, intervalSeconds, timeoutSeconds, retries);
        List<String> tags = normalizeTags(draft.tags());
        @Nullable String groupName = optionalText(draft.groupName(), MAX_GROUP_LENGTH, "groupName");
        @Nullable String description = optionalText(draft.description(), MAX_DESCRIPTION_LENGTH, "description");
        @Nullable String runbookUrl = optionalRunbookUrl(draft.runbookUrl());
        return new CheckDraft(
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

    private static String requireTarget(String target, CheckType type) {
        String trimmed = target.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_TARGET_LENGTH) {
            throw new InvalidCheckException("target", "invalid_target", "A target must have 1-2048 characters");
        }
        boolean valid = targetMatchesType(trimmed, type);
        if (!valid) {
            throw new InvalidCheckException(
                    "target", "invalid_target", "The target syntax does not match the check type");
        }
        return trimmed;
    }

    private static boolean targetMatchesType(String target, CheckType type) {
        return switch (type) {
            case HTTP -> isHttpUrl(target, "target", "invalid_target");
            case TCP -> isHostAndPort(target);
            case CERTIFICATE -> isHostName(target) || isHostAndPort(target);
        };
    }

    private static void requireInterval(int intervalSeconds) {
        if (intervalSeconds < CheckLimits.MIN_INTERVAL_SECONDS) {
            throw new InvalidCheckException(
                    "intervalSeconds", "invalid_interval", "An interval must be at least 1 second");
        }
    }

    private static void requireTimeout(int timeoutSeconds, int intervalSeconds) {
        boolean valid = timeoutSeconds >= CheckLimits.MIN_TIMEOUT_SECONDS && timeoutSeconds <= intervalSeconds;
        if (!valid) {
            throw new InvalidCheckException(
                    "timeoutSeconds", "invalid_timeout", "A timeout must be between 1 second and the interval");
        }
    }

    private static void requireRetries(int retries) {
        if (retries < 0 || retries > CheckLimits.MAX_RETRIES) {
            throw new InvalidCheckException("retries", "invalid_retries", "Retries must be between 0 and 10");
        }
    }

    private static void requireWithinPlan(PlanLimits limits, int intervalSeconds, int timeoutSeconds, int retries) {
        Integer minInterval = limits.minInterval();
        if (minInterval != null && intervalSeconds < minInterval) {
            throw new CheckOutOfPlanException("check.interval", minInterval, limits.planName());
        }
        Integer maxTimeout = limits.maxTimeout();
        if (maxTimeout != null && timeoutSeconds > maxTimeout) {
            throw new CheckOutOfPlanException("check.timeout", maxTimeout, limits.planName());
        }
        Integer maxRetries = limits.maxRetries();
        if (maxRetries != null && retries > maxRetries) {
            throw new CheckOutOfPlanException("check.retries", maxRetries, limits.planName());
        }
    }

    private static List<String> normalizeTags(List<String> tags) {
        if (tags.size() > MAX_TAGS) {
            throw new InvalidCheckException("tags", "invalid_tags", "A check may have at most 20 tags");
        }
        Map<String, String> unique = new LinkedHashMap<>();
        for (String tag : tags) {
            if (tag == null) {
                throw new InvalidCheckException("tags", "invalid_tags", "A tag must not be null");
            }
            String normalized = Normalizer.normalize(tag.strip(), Normalizer.Form.NFC);
            if (normalized.isEmpty() || normalized.length() > MAX_TAG_LENGTH) {
                throw new InvalidCheckException("tags", "invalid_tags", "A tag must have 1-50 characters");
            }
            unique.putIfAbsent(normalized.toLowerCase(Locale.ROOT), normalized);
        }
        return List.copyOf(unique.values());
    }

    private static @Nullable String optionalRunbookUrl(@Nullable String value) {
        @Nullable String trimmed = optionalText(value, MAX_RUNBOOK_LENGTH, "runbookUrl");
        if (trimmed != null && !isHttpUrl(trimmed, "runbookUrl", "invalid_runbookUrl")) {
            throw new InvalidCheckException(
                    "runbookUrl", "invalid_runbookUrl", "A runbook URL must be an http or https address");
        }
        return trimmed;
    }

    private static boolean isHttpUrl(String value, String field, String code) {
        URI uri = parseUri(value, field, code);
        String scheme = uri.getScheme();
        boolean httpScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        return httpScheme && hasHost(uri);
    }

    private static boolean hasHost(URI uri) {
        String host = uri.getHost();
        return host != null && !host.isBlank();
    }

    private static URI parseUri(String value, String field, String code) {
        try {
            return new URI(value);
        } catch (URISyntaxException exception) {
            throw new InvalidCheckException(field, code, "The value is not a valid URL", exception);
        }
    }

    private static boolean isHostAndPort(String value) {
        int colon = value.lastIndexOf(':');
        if (colon <= 0 || colon == value.length() - 1) {
            return false;
        }
        String host = value.substring(0, colon);
        String port = value.substring(colon + 1);
        return isHostName(host) && isPort(port);
    }

    private static boolean isHostName(String value) {
        if (value.isBlank()) {
            return false;
        }
        return value.chars()
                .noneMatch(character -> Character.isWhitespace(character) || character == '/' || character == ':');
    }

    private static boolean isPort(String value) {
        boolean digits = PORT_PATTERN.matcher(value).matches();
        int port = digits ? Integer.parseInt(value) : 0;
        return port >= MIN_PORT && port <= MAX_PORT;
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
