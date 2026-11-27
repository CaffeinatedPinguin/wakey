package dev.wakey.checks.internal;

import dev.wakey.checks.CheckDraft;
import dev.wakey.checks.CheckError;
import dev.wakey.checks.CheckFieldLimits;
import dev.wakey.checks.CheckLimits;
import dev.wakey.checks.CheckType;
import dev.wakey.checks.HttpUrlRule;
import dev.wakey.spaces.SpaceError;
import dev.wakey.web.ApiException;
import dev.wakey.web.DisplayText;
import dev.wakey.web.ErrorCode;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Normalizes the editable check fields and applies the domain rules the request contract cannot express. */
public final class CheckValidator {

    private static final int MIN_PORT = 1;

    private static final int MAX_PORT = 65535;

    private static final int NO_PORT = -1;

    private static final TextRule GROUP_RULE =
            new TextRule("groupName", CheckError.INVALID_GROUP_NAME, CheckFieldLimits.MAX_GROUP_LENGTH, false);

    private static final TextRule DESCRIPTION_RULE =
            new TextRule("description", CheckError.INVALID_DESCRIPTION, CheckFieldLimits.MAX_DESCRIPTION_LENGTH, true);

    private CheckValidator() {}

    public static CheckDraft validate(CheckDraft draft) {
        String name = requireName(draft.name());
        String target = requireTarget(draft.target(), draft.type());
        int intervalSeconds = draft.intervalSeconds();
        int timeoutSeconds = draft.timeoutSeconds();
        int retries = draft.retries();
        requireTimeout(timeoutSeconds, intervalSeconds);
        List<String> tags = normalizeTags(draft.tags());
        @Nullable String groupName = optionalText(draft.groupName(), GROUP_RULE);
        @Nullable String description = optionalText(draft.description(), DESCRIPTION_RULE);
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
        String normalized = DisplayText.normalize(name);
        if (!DisplayText.isValid(normalized, CheckFieldLimits.MAX_NAME_LENGTH, false)) {
            throw ApiException.invalidField(SpaceError.INVALID_NAME, "/name");
        }
        return normalized;
    }

    private static String requireTarget(String target, CheckType type) {
        String normalized = DisplayText.normalize(target);
        boolean withinLimits = DisplayText.isValid(normalized, CheckFieldLimits.MAX_TARGET_LENGTH, false);
        if (!withinLimits || !targetMatchesType(normalized, type)) {
            throw ApiException.invalidField(CheckError.INVALID_TARGET, "/target");
        }
        return normalized;
    }

    private static boolean targetMatchesType(String target, CheckType type) {
        return switch (type) {
            case HTTP -> HttpUrlRule.isHttpUrl(target);
            case TCP -> isHostAndPort(target);
            case CERTIFICATE -> isCertificateTarget(target);
        };
    }

    private static void requireTimeout(int timeoutSeconds, int intervalSeconds) {
        if (!CheckLimits.timeoutWithinInterval(timeoutSeconds, intervalSeconds)) {
            throw ApiException.invalidField(CheckError.INVALID_TIMEOUT, "/timeoutSeconds");
        }
    }

    private static List<String> normalizeTags(List<String> tags) {
        Map<String, String> unique = new LinkedHashMap<>();
        for (String tag : tags) {
            if (tag == null) {
                throw ApiException.invalidField(CheckError.INVALID_TAGS, "/tags");
            }
            String normalized = normalizeTag(tag);
            if (!DisplayText.isValid(normalized, CheckFieldLimits.MAX_TAG_LENGTH, false)) {
                throw ApiException.invalidField(CheckError.INVALID_TAGS, "/tags");
            }
            unique.putIfAbsent(normalized.toLowerCase(Locale.ROOT), normalized);
        }
        return List.copyOf(unique.values());
    }

    private static String normalizeTag(String tag) {
        String spaced = tag.replace('\u00A0', ' ');
        return DisplayText.normalize(spaced);
    }

    private static @Nullable String optionalRunbookUrl(@Nullable String value) {
        @Nullable String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        boolean withinLimits = DisplayText.isValid(normalized, CheckFieldLimits.MAX_RUNBOOK_LENGTH, false);
        if (!withinLimits || !HttpUrlRule.isHttpUrl(normalized)) {
            throw ApiException.invalidField(CheckError.INVALID_RUNBOOK_URL, "/runbookUrl");
        }
        return normalized;
    }

    private static boolean isHostAndPort(String value) {
        URI uri = targetAuthority(value);
        return isPlainAuthority(uri) && isPort(uri.getPort());
    }

    private static boolean isCertificateTarget(String value) {
        URI uri = targetAuthority(value);
        if (!isPlainAuthority(uri)) {
            return false;
        }
        int port = uri.getPort();
        return port == NO_PORT || isPort(port);
    }

    private static URI targetAuthority(String value) {
        try {
            return new URI("tcp://" + value);
        } catch (URISyntaxException exception) {
            throw ApiException.invalidField(CheckError.INVALID_TARGET, "/target", exception);
        }
    }

    private static boolean isPlainAuthority(URI uri) {
        String host = uri.getHost();
        String path = uri.getPath();
        boolean hasHost = host != null && !host.isBlank();
        boolean emptyPath = path == null || path.isEmpty();
        boolean noExtras = uri.getRawUserInfo() == null && uri.getQuery() == null && uri.getFragment() == null;
        return hasHost && emptyPath && noExtras;
    }

    private static boolean isPort(int port) {
        return port >= MIN_PORT && port <= MAX_PORT;
    }

    private static @Nullable String optionalText(@Nullable String value, TextRule rule) {
        @Nullable String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        if (!DisplayText.isValid(normalized, rule.maxLength(), rule.multiline())) {
            throw ApiException.invalidField(rule.code(), "/" + rule.pointer());
        }
        return normalized;
    }

    private static @Nullable String normalizeOptional(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String normalized = DisplayText.normalize(value);
        return normalized.isEmpty() ? null : normalized;
    }

    private record TextRule(String pointer, ErrorCode code, int maxLength, boolean multiline) {}
}
