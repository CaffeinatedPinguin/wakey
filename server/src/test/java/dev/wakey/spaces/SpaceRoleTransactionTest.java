package dev.wakey.spaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import dev.wakey.security.CurrentUser;
import dev.wakey.testsupport.PostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

@Import(SpaceRoleTransactionTest.FailingSessionsConfiguration.class)
class SpaceRoleTransactionTest extends PostgresIntegrationTest {

    private static final CurrentUser ADMIN = new CurrentUser("transaction-admin", true);

    @Test
    void aFailingSessionInvalidationRollsBackTheRoleChange() {
        // given
        SpaceService service = bean(SpaceService.class);
        String slug = uniqueSlug("rollback");
        String subject = uniqueSubject("role-change");
        service.put(ADMIN, slug, "Rollback space");
        service.addMember(ADMIN, slug, subject, SpaceRole.VIEWER);
        CurrentUser member = new CurrentUser(subject, false);

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

    @TestConfiguration(proxyBeanMethods = false)
    static class FailingSessionsConfiguration {

        @Bean
        @Primary
        UserSessions failingUserSessions(FindByIndexNameSessionRepository<? extends Session> sessions) {
            return new FailingUserSessions(sessions);
        }
    }

    private static final class FailingUserSessions extends UserSessions {

        private FailingUserSessions(FindByIndexNameSessionRepository<? extends Session> sessions) {
            super(sessions);
        }

        @Override
        public void invalidateAll(String subject) {
            throw new IllegalStateException("session store unavailable");
        }
    }
}
