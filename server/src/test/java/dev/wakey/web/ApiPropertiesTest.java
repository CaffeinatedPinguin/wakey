package dev.wakey.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiPropertiesTest {

    @Test
    void pageSizeIsCappedByConfiguration() {
        // given
        ApiProperties properties = new ApiProperties(25);

        // when
        int capped = properties.capPageSize(100);
        int belowMaximum = properties.capPageSize(10);

        // then
        assertThat(capped).isEqualTo(25);
        assertThat(belowMaximum).isEqualTo(10);
    }
}
