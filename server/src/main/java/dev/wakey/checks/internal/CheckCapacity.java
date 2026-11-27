package dev.wakey.checks.internal;

import dev.wakey.checks.CheckError;
import dev.wakey.settings.PlanProperties;
import dev.wakey.web.ApiException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Enforces the plan's check limit. */
@Component
@RequiredArgsConstructor
public class CheckCapacity {

    private final CheckStore store;

    public void require(UUID spaceId, PlanProperties plan, String planName) {
        Integer maxChecks = plan.maxChecks();
        if (maxChecks == null) {
            return;
        }
        long count = store.count(spaceId);
        if (count >= maxChecks) {
            Map<String, Object> params = Map.of("bound", maxChecks, "planName", planName);
            throw new ApiException(CheckError.PLAN_LIMIT, params);
        }
    }
}
