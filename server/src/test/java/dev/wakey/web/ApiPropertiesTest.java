package dev.wakey.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;

class ApiPropertiesTest {

    @Test
    void aDefaultPageSizeAboveTheMaximumFailsTheConfiguration() {
        // when
        Throwable thrown = catchThrowable(() -> new ApiProperties(101, 100));

        // then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }
}
