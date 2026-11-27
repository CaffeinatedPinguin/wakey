package dev.wakey.checks.internal;

import dev.wakey.checks.CheckResult;
import dev.wakey.checks.CheckState;
import dev.wakey.checks.ErrorCategory;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** One stored measurement of a check. */
@Table("check_result")
public record CheckResultEntity(
        @Id UUID id,
        UUID checkId,
        String locationId,
        Instant checkedAt,
        CheckState state,
        @Nullable ErrorCategory errorCategory,
        @Nullable Integer httpStatus,
        @Nullable Integer durationMs,
        @Nullable String protocolVersion,
        @Nullable String phases) {

    static CheckResultEntity from(UUID checkId, CheckResult result) {
        @Nullable String providedLocation = result.locationId();
        String locationId = providedLocation == null ? CheckResult.SERVER_LOCATION : providedLocation;
        return new CheckResultEntity(
                UUID.randomUUID(),
                checkId,
                locationId,
                result.checkedAt(),
                result.state(),
                result.errorCategory(),
                result.httpStatus(),
                result.durationMs(),
                result.protocolVersion(),
                result.phases());
    }

    CheckResult toResult() {
        return new CheckResult(
                locationId, checkedAt, state, errorCategory, httpStatus, durationMs, protocolVersion, phases);
    }
}
