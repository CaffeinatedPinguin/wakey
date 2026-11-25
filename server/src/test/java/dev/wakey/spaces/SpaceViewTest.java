package dev.wakey.spaces;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class SpaceViewTest {

    private static final UUID ID = UUID.randomUUID();

    private static final long VERSION = 1L;

    @Test
    void onlyEditorsAndAdminsCanWrite() {
        // when
        SpaceView admin = view(true, null);
        SpaceView editor = view(false, SpaceRole.EDITOR);
        SpaceView viewer = view(false, SpaceRole.VIEWER);
        SpaceView plainMember = view(false, null);

        // then
        assertThat(admin.canWrite()).isTrue();
        assertThat(editor.canWrite()).isTrue();
        assertThat(viewer.canWrite()).isFalse();
        assertThat(plainMember.canWrite()).isFalse();
    }

    private static SpaceView view(boolean admin, @Nullable SpaceRole role) {
        return new SpaceView(ID, "space", "Space", null, VERSION, admin, role);
    }
}
