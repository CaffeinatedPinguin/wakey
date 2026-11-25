package dev.wakey.api.checks;

import dev.wakey.checks.CheckType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** The replacement values of a check; the version makes a stale write a conflict. */
@Schema(description = "The replacement values of a check")
public record UpdateCheckRequest(
        @Schema(description = "The name, 1-100 characters after trimming", example = "API health") @NotNull
        String name,

        @Schema(description = "The protocol to measure", example = "HTTP") @NotNull
        CheckType type,

        @Schema(description = "The target address", example = "https://example.test/health") @NotNull
        String target,

        @Schema(description = "Seconds between measurements", example = "60")
        int intervalSeconds,

        @Schema(description = "Seconds before a measurement times out", example = "10")
        int timeoutSeconds,

        @Schema(description = "Retries after a failed measurement", example = "1")
        int retries,

        @Schema(description = "The optional group name") @Nullable
        String groupName,

        @Schema(description = "The optional description") @Nullable
        String description,

        @Schema(description = "The optional runbook address") @Nullable
        String runbookUrl,

        @Schema(description = "The optional tags") @NotNull List<@NotNull String> tags,

        @Schema(description = "The version the caller last read")
        long version)
        implements CheckRequestFields {}
