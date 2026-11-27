package dev.wakey.checks.internal;

import dev.wakey.checks.CheckError;
import dev.wakey.settings.PlanProperties;
import dev.wakey.web.ApiException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Enforces the plan's check limit. */
@Slf4j
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
            log.debug(
                    "Space {} reached the {} check limit of plan {} with {} checks",
                    spaceId,
                    maxChecks,
                    planName,
                    count);
            Map<String, Object> params = Map.of("bound", maxChecks, "planName", planName);
            throw new ApiException(CheckError.PLAN_LIMIT, params);
        }
    }
}
