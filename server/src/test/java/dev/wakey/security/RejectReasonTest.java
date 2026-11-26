package dev.wakey.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class RejectReasonTest {

    @Test
    void rejectReasonsAreAClosedSet() {
        // when
        List<String> reasons = Arrays.stream(RejectReason.values())
                .map(RejectReason::getWireName)
                .toList();

        // then
        assertThat(reasons)
                .containsExactlyInAnyOrder(
                        "access-denied", "csrf-token-missing", "csrf-token-invalid", "missing-authentication");
    }
}
