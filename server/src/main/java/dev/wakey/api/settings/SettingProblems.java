package dev.wakey.api.settings;

import dev.wakey.settings.TimeoutExceedsIntervalException;
import dev.wakey.web.ApiProblemException;
import dev.wakey.web.Problems;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

/** The one place that turns a settings pair violation into a Problem Details failure. */
final class SettingProblems {

    private static final String VALIDATION_FAILED = "validation_failed";

    private static final String TIMEOUT_EXCEEDS_INTERVAL = "timeout_exceeds_interval";

    private SettingProblems() {}

    static ApiProblemException timeoutExceedsInterval(TimeoutExceedsIntervalException exception) {
        Map<String, Object> error = Problems.error("/value", TIMEOUT_EXCEEDS_INTERVAL);
        List<Map<String, Object>> errors = List.of(error);
        return new ApiProblemException(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, errors, exception);
    }
}
