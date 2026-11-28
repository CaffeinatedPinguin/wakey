package dev.wakey.checks;

import java.util.UUID;

/** One check result travelling through the queue; its key keeps results of one check ordered. */
public record CheckResultMessage(UUID checkId, UUID spaceId, CheckResult result) {

    /** The queue topic of check results. */
    public static final String TOPIC = "check.result";

    /** The queue key of the message: the check id, so results of one check stay ordered. */
    public static String key(UUID checkId) {
        return checkId.toString();
    }
}
