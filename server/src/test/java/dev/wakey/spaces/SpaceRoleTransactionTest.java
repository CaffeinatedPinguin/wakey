package dev.wakey.spaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.doThrow;

import dev.wakey.testsupport.PostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class SpaceRoleTransactionTest extends PostgresIntegrationTest {

    private static final SpacePrincipal ADMIN = new SpacePrincipal("transaction-admin", true);

    @MockitoBean
    private UserSessions sessions;

    @Test
    void aFailingSessionInvalidationRollsBackTheRoleChange() {
        // given
        SpaceService service = bean(SpaceService.class);
        String slug = uniqueSlug("rollback");
        String subject = uniqueSubject("role-change");
        service.put(ADMIN, slug, "Rollback space");
        service.addMember(ADMIN, slug, subject, SpaceRole.VIEWER);
        SpacePrincipal member = new SpacePrincipal(subject, false);
        doThrow(new IllegalStateException("session store unavailable"))
                .when(sessions)
                .invalidateAll(subject);

        // when
        Throwable failure = catchThrowable(() -> service.changeRole(ADMIN, slug, subject, SpaceRole.EDITOR));
        SpaceView view = service.view(member, slug);

        // then
        assertThat(failure).isInstanceOf(IllegalStateException.class);
        assertThat(view.role()).isEqualTo(SpaceRole.VIEWER);
    }

    private static String uniqueSlug(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String uniqueSubject(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
