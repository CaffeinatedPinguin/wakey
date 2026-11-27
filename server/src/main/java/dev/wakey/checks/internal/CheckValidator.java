package dev.wakey.checks.internal;

import dev.wakey.checks.CheckDraft;
import dev.wakey.checks.CheckError;
import dev.wakey.checks.CheckFieldLimits;
import dev.wakey.checks.CheckLimits;
import dev.wakey.checks.CheckType;
import dev.wakey.checks.EgressPolicy;
import dev.wakey.checks.HttpUrlRule;
import dev.wakey.spaces.SpaceError;
import dev.wakey.web.ApiException;
import dev.wakey.web.ApiFieldError;
import dev.wakey.web.CommonError;
import dev.wakey.web.DisplayText;
import dev.wakey.web.ErrorCode;
import java.net.InetAddress;
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

    private static final int IPV4_PARTS = 4;

    private CheckValidator() {}

    public static CheckDraft validate(CheckDraft draft, EgressPolicy egressPolicy) {
        String name = requireName(draft.name());
        String target = requireTarget(draft.target(), draft.type(), egressPolicy);
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
            throw invalid(SpaceError.INVALID_NAME, "name");
        }
        return normalized;
    }

    private static String requireTarget(String target, CheckType type, EgressPolicy egressPolicy) {
        String normalized = DisplayText.normalize(target);
        boolean withinLimits = DisplayText.isValid(normalized, CheckFieldLimits.MAX_TARGET_LENGTH, false);
        if (!withinLimits || !targetMatchesType(normalized, type)) {
            throw invalid(CheckError.INVALID_TARGET, "target");
        }
        requireAllowedLiteralTarget(normalized, type, egressPolicy);
        return normalized;
    }

    private static void requireAllowedLiteralTarget(String target, CheckType type, EgressPolicy egressPolicy) {
        @Nullable String host = hostOf(target, type);
        if (host == null) {
            return;
        }
        @Nullable InetAddress address = literalAddress(host);
        if (address != null && !egressPolicy.allows(address)) {
            throw invalid(CheckError.INVALID_TARGET, "target");
        }
    }

    private static @Nullable String hostOf(String target, CheckType type) {
        URI uri = type == CheckType.HTTP ? httpUri(target) : targetAuthority(target);
        @Nullable String host = uri.getHost();
        if (host == null) {
            return null;
        }
        return stripBrackets(host);
    }

    private static URI httpUri(String value) {
        try {
            return new URI(value);
        } catch (URISyntaxException exception) {
            throw invalid(CheckError.INVALID_TARGET, "target", exception);
        }
    }

    private static String stripBrackets(String host) {
        boolean bracketed = host.startsWith("[") && host.endsWith("]");
        return bracketed ? host.substring(1, host.length() - 1) : host;
    }

    private static @Nullable InetAddress literalAddress(String host) {
        boolean literal = isIpv4Literal(host) || host.indexOf(':') >= 0;
        return literal ? parseLiteral(host) : null;
    }

    private static InetAddress parseLiteral(String host) {
        try {
            return InetAddress.ofLiteral(host);
        } catch (IllegalArgumentException exception) {
            throw invalid(CheckError.INVALID_TARGET, "target", exception);
        }
    }

    private static boolean isIpv4Literal(String host) {
        String[] parts = host.split("\\.", -1);
        boolean shaped = parts.length == IPV4_PARTS;
        for (int index = 0; shaped && index < parts.length; index++) {
            shaped = isDecimalOctet(parts[index]);
        }
        return shaped;
    }

    private static boolean isDecimalOctet(String part) {
        boolean shaped = !part.isEmpty() && part.length() <= 3;
        for (int index = 0; shaped && index < part.length(); index++) {
            char character = part.charAt(index);
            shaped = character >= '0' && character <= '9';
        }
        return shaped;
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
            throw invalid(CheckError.INVALID_TIMEOUT, "timeoutSeconds");
        }
    }

    private static List<String> normalizeTags(List<String> tags) {
        Map<String, String> unique = new LinkedHashMap<>();
        for (String tag : tags) {
            if (tag == null) {
                throw invalid(CheckError.INVALID_TAGS, "tags");
            }
            String normalized = normalizeTag(tag);
            if (!DisplayText.isValid(normalized, CheckFieldLimits.MAX_TAG_LENGTH, false)) {
                throw invalid(CheckError.INVALID_TAGS, "tags");
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
            throw invalid(CheckError.INVALID_RUNBOOK_URL, "runbookUrl");
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
            throw invalid(CheckError.INVALID_TARGET, "target", exception);
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
            throw invalid(rule.code(), rule.pointer());
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

    private static ApiException invalid(ErrorCode code, String field) {
        ApiFieldError problem = new ApiFieldError("/" + field, code.getCode(), Map.of());
        return new ApiException(CommonError.VALIDATION_FAILED, List.of(problem));
    }

    private static ApiException invalid(ErrorCode code, String field, Throwable cause) {
        ApiFieldError problem = new ApiFieldError("/" + field, code.getCode(), Map.of());
        return new ApiException(CommonError.VALIDATION_FAILED, List.of(problem), Map.of(), cause);
    }

    private record TextRule(String pointer, ErrorCode code, int maxLength, boolean multiline) {}
}
