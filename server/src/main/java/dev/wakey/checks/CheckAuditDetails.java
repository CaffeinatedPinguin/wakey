package dev.wakey.checks;

import java.util.ArrayList;
import java.util.List;
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

    static String created() {
        return json(CREATED_FIELDS);
    }

    static String updated(Check existing, CheckDraft changes) {
        List<String> changed = new ArrayList<>(metadataChanged(existing, changes));
        changed.addAll(optionalChanged(existing, changes));
        return json(changed);
    }

    static String paused(PausedReason reason) {
        return "{\"changed\":[\"paused\"],\"pausedReason\":\"" + reason.name() + "\"}";
    }

    static String resumed() {
        return "{\"changed\":[\"paused\",\"pausedReason\"]}";
    }

    static String deleted() {
        return json(List.of("deleted"));
    }

    static String bulk(CheckBulkAction action, int affected) {
        return "{\"action\":\"" + action.name() + "\",\"affected\":" + affected + "}";
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

    private static String json(List<String> fields) {
        if (fields.isEmpty()) {
            return "{\"changed\":[]}";
        }
        String joined = String.join("\",\"", fields);
        return "{\"changed\":[\"" + joined + "\"]}";
    }
}
