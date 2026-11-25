package dev.wakey.spaces.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SpacesControllerPlacementTest {

    @Test
    void spacesControllerLivesInTheSpacesWebPackage() {
        // when
        String packageName = SpacesController.class.getPackageName();

        // then
        assertThat(packageName).isEqualTo("dev.wakey.spaces.web");
    }
}
