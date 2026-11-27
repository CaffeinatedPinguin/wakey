package dev.wakey.checks;

import dev.wakey.checks.internal.CheckResultStore;
import dev.wakey.messaging.MessageQueue;
import dev.wakey.web.ApiException;
import dev.wakey.web.ApiFieldError;
import dev.wakey.web.CommonError;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** The append-only results of checks and the queue they travel through. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckResults {

    private final CheckResultStore store;

    private final MessageQueue queue;

    private final ObjectMapper objectMapper;

    /**
     * Publishes one result as part of the caller's transaction. A caller that does not name a
     * location gets the server location.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(UUID checkId, UUID spaceId, CheckResult result) {
        CheckResult located = result.locationId() == null ? result.withLocation(CheckResult.SERVER_LOCATION) : result;
        CheckResultMessage message = new CheckResultMessage(checkId, spaceId, located);
        queue.publish(
                CheckResultMessage.TOPIC, CheckResultMessage.key(checkId), objectMapper.writeValueAsString(message));
    }

    /**
     * Stores a message consumed from the queue. A repeated message is ignored by the check and
     * time key, so at-least-once delivery never creates a second result or a second interval.
     */
    @Transactional
    public boolean recordFromMessage(CheckResultMessage message) {
        UUID checkId = message.checkId();
        CheckResult result = message.result();
        if (store.hasResult(checkId, result.checkedAt())) {
            return false;
        }
        boolean hasSuccessfulResult = store.hasSuccessfulResult(checkId);
        CheckOutcome outcome = CheckStatePolicy.evaluate(result, hasSuccessfulResult);
        if (outcome.needsConfigurationReview()) {
            log.debug("Check {} needs a configuration review", checkId);
        }
        CheckResult effective = result.withState(outcome.state());
        store.record(checkId, effective);
        return true;
    }

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
