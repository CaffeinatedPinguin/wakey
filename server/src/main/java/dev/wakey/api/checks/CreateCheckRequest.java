package dev.wakey.api.checks;

import dev.wakey.checks.CheckType;
import dev.wakey.checks.NewCheck;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** The desired state of a new check; the domain rechecks every rule after binding. */
@Schema(description = "The desired state of a new check")
public record CreateCheckRequest(
        @Schema(description = "The name, 1-100 characters after trimming", example = "API health")
        @NotBlank
        @Size(max = 100)
        String name,

        @Schema(description = "The protocol to measure", example = "HTTP") @NotNull
        CheckType type,

        @Schema(description = "The target address", example = "https://example.test/health") @NotBlank @Size(max = 2048)
        String target,

        @Schema(description = "Seconds between measurements", example = "60")
        int intervalSeconds,

        @Schema(description = "Seconds before a measurement times out", example = "10")
        int timeoutSeconds,

        @Schema(description = "Retries after a failed measurement", example = "1")
        int retries,

        @Schema(description = "The optional group name") @Size(max = 100) @Nullable
        String groupName,

        @Schema(description = "The optional description") @Size(max = 1000) @Nullable
        String description,

        @Schema(description = "The optional runbook address") @Size(max = 2048) @Nullable
        String runbookUrl,

        @Schema(description = "The optional tags") @NotNull List<String> tags) {

    NewCheck toNewCheck() {
        return new NewCheck(
                name, type, target, intervalSeconds, timeoutSeconds, retries, groupName, description, runbookUrl, tags);
    }
}
