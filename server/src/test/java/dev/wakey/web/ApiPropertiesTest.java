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

    @Test
    void pageSizeIsAlwaysBetweenOneAndTheMaximum() {
        // given
        ApiProperties properties = new ApiProperties(20, 100);

        // when
        int belowMinimum = properties.pageSize(0);
        int negative = properties.pageSize(-5);
        int aboveMaximum = properties.pageSize(101);
        int missing = properties.pageSize(null);
        int inRange = properties.pageSize(50);

        // then
        assertThat(belowMinimum).isEqualTo(1);
        assertThat(negative).isEqualTo(1);
        assertThat(aboveMaximum).isEqualTo(100);
        assertThat(missing).isEqualTo(20);
        assertThat(inRange).isEqualTo(50);
    }
}
