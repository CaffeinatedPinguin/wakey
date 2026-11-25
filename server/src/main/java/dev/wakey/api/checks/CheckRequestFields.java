package dev.wakey.api.checks;

import dev.wakey.checks.CheckDraft;
import dev.wakey.checks.CheckType;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** The editable fields shared by the create and update requests; only the update adds a version. */
interface CheckRequestFields {

    String name();

    CheckType type();

    String target();

    int intervalSeconds();

    int timeoutSeconds();

    int retries();

    @Nullable
    String groupName();

    @Nullable
    String description();

    @Nullable
    String runbookUrl();

    List<String> tags();

    default CheckDraft toDraft() {
        return new CheckDraft(
                name(),
                type(),
                target(),
                intervalSeconds(),
                timeoutSeconds(),
                retries(),
                groupName(),
                description(),
                runbookUrl(),
                tags());
    }
}
