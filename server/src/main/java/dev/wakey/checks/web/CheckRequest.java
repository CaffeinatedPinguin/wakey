package dev.wakey.checks.web;

import dev.wakey.checks.CheckActor;
import dev.wakey.checks.CheckDraft;
import dev.wakey.checks.CheckFieldLimits;
import dev.wakey.checks.CheckLimits;
import dev.wakey.checks.CheckType;
import dev.wakey.checks.CheckUpdate;
import dev.wakey.spaces.web.SpaceWriter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** The editable fields of a check; the request contract carries their bounds. */
@Schema(description = "The editable fields of a check")
public record CheckRequest(
        @Schema(
                description = "The name, " + CheckFieldLimits.MIN_TEXT_LENGTH + "-" + CheckFieldLimits.MAX_NAME_LENGTH
                        + " characters after trimming",
                example = "API health")
        @NotNull
        @Size(min = CheckFieldLimits.MIN_TEXT_LENGTH, max = CheckFieldLimits.MAX_NAME_LENGTH)
        String name,

        @Schema(
                description = "The protocol to measure",
                example = "HTTP",
                implementation = CheckType.class,
                enumAsRef = true)
        @NotNull
        CheckType type,

        @Schema(description = "The target address", example = "https://example.test/health")
        @NotNull
        @Size(min = CheckFieldLimits.MIN_TEXT_LENGTH, max = CheckFieldLimits.MAX_TARGET_LENGTH)
        String target,

        @Schema(description = "Seconds between measurements", example = "60") @Min(CheckLimits.MIN_INTERVAL_SECONDS)
        int intervalSeconds,

        @Schema(description = "Seconds before a measurement times out", example = "10")
        @Min(CheckLimits.MIN_TIMEOUT_SECONDS)
        int timeoutSeconds,

        @Schema(description = "Retries after a failed measurement", example = "1")
        @Min(CheckLimits.MIN_RETRIES)
        @Max(CheckLimits.MAX_RETRIES)
        int retries,

        @Schema(description = "The optional group name") @Nullable @Size(max = CheckFieldLimits.MAX_GROUP_LENGTH)
        String groupName,

        @Schema(description = "The optional description") @Nullable @Size(max = CheckFieldLimits.MAX_DESCRIPTION_LENGTH)
        String description,

        @Schema(description = "The optional runbook address")
        @Nullable
        @Size(max = CheckFieldLimits.MAX_RUNBOOK_LENGTH)
        @HttpUrl
        String runbookUrl,

        @Schema(description = "The optional tags") @NotNull @Size(max = CheckFieldLimits.MAX_TAGS)
        List<@NotNull @Size(min = CheckFieldLimits.MIN_TEXT_LENGTH, max = CheckFieldLimits.MAX_TAG_LENGTH) String>
                tags) {

    CheckDraft toDraft() {
        return new CheckDraft(
                name, type, target, intervalSeconds, timeoutSeconds, retries, groupName, description, runbookUrl, tags);
    }

    CheckUpdate toUpdate(UUID id, long expectedVersion) {
        return new CheckUpdate(id, toDraft(), expectedVersion);
    }

    /** The one place that turns the resolved writer of a write into the actor of the audit entry. */
    static CheckActor actorOf(SpaceWriter writer) {
        var user = writer.user();
        return CheckActor.of(user.subject(), writer.viaToken());
    }
}
