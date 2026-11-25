package dev.wakey.api.checks;

import dev.wakey.checks.Check;
import dev.wakey.checks.CheckDraft;
import dev.wakey.checks.CheckOutOfPlanException;
import dev.wakey.checks.CheckPlanLimitException;
import dev.wakey.checks.CheckResult;
import dev.wakey.checks.CheckResults;
import dev.wakey.checks.CheckService;
import dev.wakey.checks.CheckUpdate;
import dev.wakey.checks.PlanLimits;
import dev.wakey.settings.PlanProperties;
import dev.wakey.settings.SpaceSettings;
import dev.wakey.spaces.SpaceView;
import dev.wakey.web.ApiProblemException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Reads one check with its newest result and changes the checks of a space. */
@Component
final class ChecksCommands {

    private static final String OUT_OF_PLAN = "out_of_plan";

    private static final String PLAN_LIMIT = "plan_limit";

    private final CheckService checks;

    private final CheckResults results;

    private final ChecksAccess access;

    private final SpaceSettings settings;

    ChecksCommands(CheckService checks, CheckResults results, ChecksAccess access, SpaceSettings settings) {
        this.checks = checks;
        this.results = results;
        this.access = access;
        this.settings = settings;
    }

    CheckResponse get(Authentication authentication, String slug, UUID id) {
        SpaceView space = access.read(authentication, slug);
        return read(space, id);
    }

    CheckResponse create(Authentication authentication, String slug, CreateCheckRequest request) {
        SpaceView space = access.write(authentication, slug);
        CheckDraft draft = request.toDraft();
        PlanLimits limits = planLimits(space);
        try {
            Check created = checks.create(space.id(), draft, limits);
            return CheckResponse.from(created);
        } catch (CheckOutOfPlanException | CheckPlanLimitException exception) {
            throw planProblem(exception);
        }
    }

    CheckResponse update(Authentication authentication, String slug, UUID id, UpdateCheckRequest request) {
        SpaceView space = access.write(authentication, slug);
        CheckDraft changes = request.toDraft();
        PlanLimits limits = planLimits(space);
        long expectedVersion = request.version();
        CheckUpdate update = new CheckUpdate(id, changes, expectedVersion, limits);
        try {
            Check updated = checks.update(space.id(), update);
            return CheckResponse.from(updated);
        } catch (CheckOutOfPlanException | CheckPlanLimitException exception) {
            throw planProblem(exception);
        }
    }

    CheckResponse pause(Authentication authentication, String slug, UUID id, PauseCheckRequest request) {
        SpaceView space = access.write(authentication, slug);
        checks.pause(space.id(), id, request.reason());
        return read(space, id);
    }

    CheckResponse resume(Authentication authentication, String slug, UUID id) {
        SpaceView space = access.write(authentication, slug);
        checks.resume(space.id(), id);
        return read(space, id);
    }

    void delete(Authentication authentication, String slug, UUID id) {
        SpaceView space = access.write(authentication, slug);
        checks.delete(space.id(), id);
    }

    private PlanLimits planLimits(SpaceView space) {
        String planName = settings.resolvePlanName(space.planName());
        PlanProperties plan = settings.plan(planName);
        Integer minInterval = plan.minInterval();
        Integer maxTimeout = plan.maxTimeout();
        Integer maxRetries = plan.maxRetries();
        Integer maxChecks = plan.maxChecks();
        return new PlanLimits(minInterval, maxTimeout, maxRetries, maxChecks, planName);
    }

    private CheckResponse read(SpaceView space, UUID id) {
        Check check = checks.get(space.id(), id);
        Optional<CheckResult> latest = results.latest(id);
        return CheckResponse.from(check, latest.orElse(null));
    }

    private static ApiProblemException planProblem(RuntimeException exception) {
        if (exception instanceof CheckOutOfPlanException outOfPlan) {
            long bound = outOfPlan.bound();
            String planName = outOfPlan.planName();
            String key = outOfPlan.key();
            Map<String, Object> params = Map.of("bound", bound, "planName", planName, "key", key);
            return new ApiProblemException(HttpStatus.BAD_REQUEST, OUT_OF_PLAN, params, exception);
        }
        CheckPlanLimitException planLimit = (CheckPlanLimitException) exception;
        long bound = planLimit.bound();
        String planName = planLimit.planName();
        Map<String, Object> params = Map.of("bound", bound, "planName", planName);
        return new ApiProblemException(HttpStatus.BAD_REQUEST, PLAN_LIMIT, params, exception);
    }
}
