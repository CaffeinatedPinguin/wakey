package dev.wakey.checks;

import dev.wakey.checks.internal.CheckResultStore;
import dev.wakey.web.ApiException;
import dev.wakey.web.ApiFieldError;
import dev.wakey.web.CommonError;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The append-only results of checks. */
@Service
@RequiredArgsConstructor
public class CheckResults {

    private final CheckResultStore store;

    /**
     * Appends one result. A {@link CheckState#DOWN} state with the
     * {@link ErrorCategory#LOCATION_NETWORK} category is rejected, because a location that cannot
     * reach the network does not prove the target is down.
     */
    @Transactional
    public void record(UUID checkId, CheckResult result) {
        if (result.state() == CheckState.DOWN && result.errorCategory() == ErrorCategory.LOCATION_NETWORK) {
            ApiFieldError problem =
                    new ApiFieldError("/state", CheckError.LOCATION_ERROR_CANNOT_BE_DOWN.getCode(), Map.of());
            throw new ApiException(CommonError.VALIDATION_FAILED, List.of(problem));
        }
        store.record(checkId, result);
    }

    /** The newest result of the check, if any. */
    public Optional<CheckResult> latest(UUID checkId) {
        return store.latest(checkId);
    }

    /** The newest results of the check, newest first. */
    public List<CheckResult> recent(UUID checkId, int limit) {
        return store.recent(checkId, limit);
    }
}
