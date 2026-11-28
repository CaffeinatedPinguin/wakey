package dev.wakey.tokens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import dev.wakey.audit.AuditLog;
import dev.wakey.spaces.SpaceMember;
import dev.wakey.spaces.SpaceMemberId;
import dev.wakey.spaces.SpaceMembers;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceService;
import dev.wakey.spaces.SpaceView;
import dev.wakey.testsupport.PostgresIntegrationTest;
import dev.wakey.tokens.internal.TokenProperties;
import dev.wakey.tokens.internal.TokenRepository;
import dev.wakey.tokens.internal.TokenStore;
import dev.wakey.web.CurrentUser;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;

@Import(MemberRemovalRollbackIntegrationTest.FailingTokenStoreConfiguration.class)
class MemberRemovalRollbackIntegrationTest extends PostgresIntegrationTest {

    private static final String ISSUER = "https://issuer.example.com";

    private static final CurrentUser ADMIN = new CurrentUser(ISSUER, "rollback-admin", true);

    @Test
    void aFailedRevocationKeepsTheMembership() {
        // given
        SpaceService service = bean(SpaceService.class);
        SpaceMembers members = bean(SpaceMembers.class);
        String slug = uniqueSlug("rollback");
        String subject = uniqueSubject("removed");
        service.put(ADMIN, slug, "Rollback space", null);
        members.addMember(ADMIN, slug, new SpaceMember(ISSUER, subject, SpaceRole.VIEWER), false);
        CurrentUser member = new CurrentUser(ISSUER, subject, false);

        // when
        Throwable failure =
                catchThrowable(() -> members.removeMember(ADMIN, slug, new SpaceMemberId(ISSUER, subject), false));
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
    static class FailingTokenStoreConfiguration {

        @Bean
        @Primary
        TokenStore failingTokenStore(
                TokenRepository tokenRows,
                JdbcAggregateTemplate aggregates,
                AuditLog audit,
                TokenProperties properties) {
            return new FailingTokenStore(tokenRows, aggregates, audit, properties);
        }
    }

    static class FailingTokenStore extends TokenStore {

        FailingTokenStore(
                TokenRepository tokenRows,
                JdbcAggregateTemplate aggregates,
                AuditLog audit,
                TokenProperties properties) {
            super(tokenRows, aggregates, audit, properties);
        }

        @Override
        public int revokeAll(UUID spaceId, String ownerIssuer, String ownerSubject, Instant now) {
            throw new IllegalStateException("token store unavailable");
        }
    }
}
