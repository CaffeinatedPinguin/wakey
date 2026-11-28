package dev.wakey.tokens.internal;

import static org.assertj.core.api.Assertions.assertThat;

import dev.wakey.spaces.Space;
import dev.wakey.spaces.SpaceMember;
import dev.wakey.spaces.SpaceMembers;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceService;
import dev.wakey.testsupport.PostgresIntegrationTest;
import dev.wakey.testsupport.QueryCountingConfiguration;
import dev.wakey.testsupport.QueryCountingDataSource;
import dev.wakey.tokens.ApiTokens;
import dev.wakey.tokens.CreatedToken;
import dev.wakey.tokens.TokenIdentity;
import dev.wakey.tokens.TokenOptions;
import dev.wakey.tokens.TokenOwner;
import dev.wakey.tokens.TokenScope;
import dev.wakey.web.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

@Import(QueryCountingConfiguration.class)
class TokenQueryCountTest extends PostgresIntegrationTest {

    private static final String ISSUER = "https://issuer.example.com";

    private static final CurrentUser ADMIN = new CurrentUser(ISSUER, "token-queries-admin", true);

    @Test
    void aTokenRequestReadsItsSpaceOnce() {
        // given
        SpaceService spaces = bean(SpaceService.class);
        SpaceMembers members = bean(SpaceMembers.class);
        ApiTokens tokens = bean(ApiTokens.class);
        QueryCountingDataSource dataSource = bean(QueryCountingDataSource.class);
        String slug = uniqueSlug("space-reads");
        spaces.put(ADMIN, slug, "Space reads", null);
        String subject = uniqueSubject("space-reads-editor");
        members.addMember(ADMIN, slug, new SpaceMember(ISSUER, subject, SpaceRole.EDITOR), false);
        TokenOwner owner = new TokenOwner(ISSUER, subject);
        CreatedToken created = tokens.create(owner, slug, "reads", new TokenOptions(TokenScope.READ, null));

        // when
        dataSource.reset();
        Optional<TokenIdentity> identity = tokens.authenticate(created.secret());

        // then
        assertThat(identity).isPresent();
        assertThat(dataSource.selectCount("space")).isEqualTo(1);
    }

    @Test
    void revokingAMembersTokensUsesAConstantNumberOfQueries() {
        // given
        SpaceService spaces = bean(SpaceService.class);
        SpaceMembers members = bean(SpaceMembers.class);
        ApiTokens tokens = bean(ApiTokens.class);
        TokenStore store = bean(TokenStore.class);
        QueryCountingDataSource dataSource = bean(QueryCountingDataSource.class);
        String slug = uniqueSlug("revoke-queries");
        Space space = spaces.put(ADMIN, slug, "Revoke queries", null).space();
        UUID spaceId = space.id();
        String fewSubject = uniqueSubject("few");
        String manySubject = uniqueSubject("many");
        members.addMember(ADMIN, slug, new SpaceMember(ISSUER, fewSubject, SpaceRole.EDITOR), false);
        members.addMember(ADMIN, slug, new SpaceMember(ISSUER, manySubject, SpaceRole.EDITOR), false);
        seedTokens(tokens, slug, fewSubject, 2);
        seedTokens(tokens, slug, manySubject, 10);
        Instant now = Instant.now(bean(Clock.class));

        // when
        dataSource.reset();
        store.revokeAll(spaceId, ISSUER, fewSubject, now);
        int fewReads = dataSource.selectCount();
        dataSource.reset();
        store.revokeAll(spaceId, ISSUER, manySubject, now);
        int manyReads = dataSource.selectCount();
        long fewRevoked = countRevoked(spaceId, fewSubject);
        long manyRevoked = countRevoked(spaceId, manySubject);

        // then
        assertThat(fewReads).isEqualTo(1);
        assertThat(manyReads).isEqualTo(1);
        assertThat(fewRevoked).isEqualTo(2);
        assertThat(manyRevoked).isEqualTo(10);
    }

    private static void seedTokens(ApiTokens tokens, String slug, String subject, int count) {
        TokenOwner owner = new TokenOwner(ISSUER, subject);
        for (int index = 0; index < count; index++) {
            tokens.create(owner, slug, "token-" + index, new TokenOptions(TokenScope.READ, null));
        }
    }

    private long countRevoked(UUID spaceId, String subject) {
        return jdbc().sql("""
                        SELECT count(*) FROM api_token
                        WHERE space_id = :spaceId AND owner_subject = :subject AND revoked_at IS NOT NULL
                        """)
                .param("spaceId", spaceId)
                .param("subject", subject)
                .query(Long.class)
                .single();
    }

    private static String uniqueSlug(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String uniqueSubject(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
