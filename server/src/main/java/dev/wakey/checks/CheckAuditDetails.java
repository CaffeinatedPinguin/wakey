package dev.wakey.checks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Builds the short audit payload of a check change; it names the fields, never their secrets. */
final class CheckAuditDetails {

    private static final List<String> CREATED_FIELDS = List.of(
            "name",
            "type",
            "target",
            "intervalSeconds",
            "timeoutSeconds",
            "retries",
            "groupName",
            "description",
            "runbookUrl",
            "tags");

    private CheckAuditDetails() {}

    static Map<String, Object> created() {
        return Map.<String, Object>of("changed", CREATED_FIELDS);
    }

    static Map<String, Object> updated(Check existing, CheckDraft changes) {
        List<String> changed = new ArrayList<>(metadataChanged(existing, changes));
        changed.addAll(optionalChanged(existing, changes));
        return Map.<String, Object>of("changed", changed);
    }

    static Map<String, Object> paused(PausedReason reason) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("changed", List.of("paused"));
        details.put("pausedReason", reason.name());
        return details;
    }

    static Map<String, Object> resumed() {
        return Map.<String, Object>of("changed", List.of("paused", "pausedReason"));
    }

    static Map<String, Object> deleted() {
        return Map.<String, Object>of("changed", List.of("deleted"));
    }

    static Map<String, Object> bulk(CheckBulkAction action, int affected) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("action", action.name());
        details.put("affected", affected);
        return details;
    }

    private static List<String> metadataChanged(Check existing, CheckDraft changes) {
        List<String> changed = new ArrayList<>();
        if (!existing.name().equals(changes.name())) {
            changed.add("name");
        }
        if (existing.type() != changes.type()) {
            changed.add("type");
        }
        if (!existing.target().equals(changes.target())) {
            changed.add("target");
        }
        if (existing.intervalSeconds() != changes.intervalSeconds()) {
            changed.add("intervalSeconds");
        }
        if (existing.timeoutSeconds() != changes.timeoutSeconds()) {
            changed.add("timeoutSeconds");
        }
        if (existing.retries() != changes.retries()) {
            changed.add("retries");
        }
        return changed;
    }

    private static List<String> optionalChanged(Check existing, CheckDraft changes) {
        List<String> changed = new ArrayList<>();
        if (!same(existing.groupName(), changes.groupName())) {
            changed.add("groupName");
        }
        if (!same(existing.description(), changes.description())) {
            changed.add("description");
        }
        if (!same(existing.runbookUrl(), changes.runbookUrl())) {
            changed.add("runbookUrl");
        }
        if (!existing.tags().equals(changes.tags())) {
            changed.add("tags");
        }
        return changed;
    }

    private static boolean same(@Nullable String left, @Nullable String right) {
        return left == null ? right == null : left.equals(right);
    }
}
