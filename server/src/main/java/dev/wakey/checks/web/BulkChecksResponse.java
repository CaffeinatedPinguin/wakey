package dev.wakey.checks.web;

import io.swagger.v3.oas.annotations.media.Schema;

/** The bulk result: how many checks the action really changed. */
@Schema(description = "The result of a bulk action")
public record BulkChecksResponse(
        @Schema(
                description = "How many checks the action changed",
                example = "2",
                requiredMode = Schema.RequiredMode.REQUIRED)
        int affected) {

    static BulkChecksResponse from(int affected) {
        return new BulkChecksResponse(affected);
    }
}
