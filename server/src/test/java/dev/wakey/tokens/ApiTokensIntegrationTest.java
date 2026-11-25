package dev.wakey.tokens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import dev.wakey.spaces.PutResult;
import dev.wakey.spaces.Space;
import dev.wakey.spaces.SpaceAccessDeniedException;
import dev.wakey.spaces.SpaceNotFoundException;
import dev.wakey.spaces.SpacePrincipal;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceService;
import dev.wakey.testsupport.PostgresIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApiTokensIntegrationTest extends PostgresIntegrationTest {

    private static final SpacePrincipal ADMIN = new SpacePrincipal("fixture-admin", true);

    private static final SpacePrincipal EDITOR = new SpacePrincipal("fixture-editor", false);

    private static final SpacePrincipal VIEWER = new SpacePrincipal("fixture-viewer", false);

    private static final TokenOwner EDITOR_OWNER = new TokenOwner(EDITOR.subject());

    private static final TokenOwner VIEWER_OWNER = new TokenOwner(VIEWER.subject());

    @Test
    void secretIsShownOnceAndOnlyTheHashIsStored() {
        // given
        Space space = seedSpaceWithMembers("secret");
        String slug = space.slug();

        // when
        CreatedToken created =
                tokens().create(EDITOR_OWNER, slug, "CI token", new TokenOptions(TokenScope.WRITE, null));
        String secret = created.secret();
        TokenSummary summary = created.token();
        String tokenHash = jdbc().sql("SELECT token_hash FROM api_token WHERE id = :id")
                .param("id", summary.id())
                .query(String.class)
                .single();
        String storedRow = jdbc().sql("SELECT token_hash || ':' || name || ':' || owner_subject || ':' || scope"
                        + " FROM api_token WHERE id = :id")
                .param("id", summary.id())
                .query(String.class)
                .single();

        // then
        assertThat(secret).startsWith("wk_").hasSize(46);
        assertThat(tokenHash).hasSize(64).matches("[0-9a-f]{64}").isEqualTo(sha256Hex(secret));
        assertThat(storedRow).doesNotContain(secret);
        assertThat(created.toString()).doesNotContain(secret);
    }

    @Test
    void authenticateAcceptsAValidToken() {
        // given
        Space space = seedSpaceWithMembers("valid");
        String slug = space.slug();
        CreatedToken created =
                tokens().create(EDITOR_OWNER, slug, "CI token", new TokenOptions(TokenScope.WRITE, null));

        // when
        Optional<TokenIdentity> found = tokens().authenticate(created.secret());

        // then
        TokenIdentity identity = found.orElseThrow();
        TokenSummary summary = created.token();
        assertThat(identity.tokenId()).isEqualTo(summary.id());
        assertThat(identity.spaceId()).isEqualTo(space.id());
        assertThat(identity.ownerSubject()).isEqualTo(EDITOR.subject());
        assertThat(identity.scope()).isEqualTo(TokenScope.WRITE);
    }

    @Test
    void expiredTokenIsRejected() {
        // given
        Clock clock = bean(Clock.class);
        Space space = seedSpaceWithMembers("expired");
        String slug = space.slug();
        Instant expiredAt = Instant.now(clock).minusSeconds(60);
        CreatedToken created =
                tokens().create(EDITOR_OWNER, slug, "expired", new TokenOptions(TokenScope.READ, expiredAt));

        // when
        Optional<TokenIdentity> found = tokens().authenticate(created.secret());

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void revokedTokenIsRejected() {
        // given
        Space space = seedSpaceWithMembers("revoked");
        String slug = space.slug();
        CreatedToken created = tokens().create(EDITOR_OWNER, slug, "revoked", new TokenOptions(TokenScope.WRITE, null));
        TokenSummary summary = created.token();

        // when
        tokens().revoke(EDITOR_OWNER, slug, summary.id());
        Optional<TokenIdentity> found = tokens().authenticate(created.secret());
        List<TokenSummary> listed = tokens().list(EDITOR_OWNER, slug);

        // then
        TokenSummary stored = listed.get(0);
        assertThat(found).isEmpty();
        assertThat(stored.revokedAt()).isNotNull();
    }

    @Test
    void viewerCannotCreateAWriteToken() {
        // given
        Space space = seedSpaceWithMembers("viewer-write");
        String slug = space.slug();

        // when
        Throwable denied = catchThrowable(
                () -> tokens().create(VIEWER_OWNER, slug, "write", new TokenOptions(TokenScope.WRITE, null)));
        CreatedToken read = tokens().create(VIEWER_OWNER, slug, "read", new TokenOptions(TokenScope.READ, null));

        // then
        TokenScope createdScope = read.token().scope();
        assertThat(denied).isInstanceOf(SpaceAccessDeniedException.class);
        assertThat(createdScope).isEqualTo(TokenScope.READ);
    }

    @Test
    void loweringTheRoleLowersTheEffectiveScope() {
        // given
        Space space = seedSpaceWithMembers("lower-role");
        String slug = space.slug();
        CreatedToken created = tokens().create(EDITOR_OWNER, slug, "writer", new TokenOptions(TokenScope.WRITE, null));

        // when
        Optional<TokenIdentity> before = tokens().authenticate(created.secret());
        spaces().changeRole(ADMIN, slug, EDITOR.subject(), SpaceRole.VIEWER);
        Optional<TokenIdentity> after = tokens().authenticate(created.secret());

        // then
        TokenScope scopeBefore = before.orElseThrow().scope();
        TokenScope scopeAfter = after.orElseThrow().scope();
        assertThat(scopeBefore).isEqualTo(TokenScope.WRITE);
        assertThat(scopeAfter).isEqualTo(TokenScope.READ);
    }

    @Test
    void removalFromTheSpaceInvalidatesTheToken() {
        // given
        Space space = seedSpaceWithMembers("removed-member");
        String slug = space.slug();
        CreatedToken created = tokens().create(EDITOR_OWNER, slug, "ci", new TokenOptions(TokenScope.WRITE, null));

        // when
        Optional<TokenIdentity> beforeRemoval = tokens().authenticate(created.secret());
        spaces().removeMember(ADMIN, slug, EDITOR.subject());
        Optional<TokenIdentity> afterRemoval = tokens().authenticate(created.secret());

        // then
        assertThat(beforeRemoval).isPresent();
        assertThat(afterRemoval).isEmpty();
    }

    @Test
    void unknownOrWrongPrefixIsRejected() {
        // given
        Space space = seedSpaceWithMembers("unknown");
        String slug = space.slug();
        CreatedToken created = tokens().create(EDITOR_OWNER, slug, "known", new TokenOptions(TokenScope.READ, null));
        String wrongPrefix = "wka_" + created.secret();

        // when
        Optional<TokenIdentity> empty = tokens().authenticate("");
        Optional<TokenIdentity> wrong = tokens().authenticate(wrongPrefix);
        Optional<TokenIdentity> random = tokens().authenticate("wk_" + UUID.randomUUID());
        Optional<TokenIdentity> valid = tokens().authenticate(created.secret());

        // then
        assertThat(empty).isEmpty();
        assertThat(wrong).isEmpty();
        assertThat(random).isEmpty();
        assertThat(valid).isPresent();
    }

    @Test
    void listNeverReturnsSecrets() {
        // given
        Space space = seedSpaceWithMembers("list");
        String slug = space.slug();
        CreatedToken first = tokens().create(EDITOR_OWNER, slug, "first", new TokenOptions(TokenScope.READ, null));
        CreatedToken second = tokens().create(EDITOR_OWNER, slug, "second", new TokenOptions(TokenScope.WRITE, null));
        CreatedToken viewerToken =
                tokens().create(VIEWER_OWNER, slug, "viewer", new TokenOptions(TokenScope.READ, null));

        // when
        List<TokenSummary> editorTokens = tokens().list(EDITOR_OWNER, slug);
        List<TokenSummary> viewerTokens = tokens().list(VIEWER_OWNER, slug);
        String rendered = editorTokens.toString();

        // then
        assertThat(editorTokens).extracting(TokenSummary::name).containsExactlyInAnyOrder("first", "second");
        assertThat(viewerTokens).extracting(TokenSummary::name).containsExactly("viewer");
        assertThat(rendered).doesNotContain(first.secret(), second.secret(), viewerToken.secret());
    }

    @Test
    void lastUsedIsUpdated() {
        // given
        Space space = seedSpaceWithMembers("last-used");
        String slug = space.slug();
        CreatedToken created = tokens().create(EDITOR_OWNER, slug, "used", new TokenOptions(TokenScope.READ, null));

        // when
        List<TokenSummary> before = tokens().list(EDITOR_OWNER, slug);
        tokens().authenticate(created.secret());
        List<TokenSummary> after = tokens().list(EDITOR_OWNER, slug);

        // then
        TokenSummary beforeUse = before.get(0);
        TokenSummary afterUse = after.get(0);
        assertThat(beforeUse.lastUsedAt()).isNull();
        assertThat(afterUse.lastUsedAt()).isNotNull();
    }

    @Test
    void fullLifecycleNarrowsTheScopeAndThenInvalidatesTheToken() {
        // given
        Space space = seedSpaceWithMembers("lifecycle");
        String slug = space.slug();
        CreatedToken created =
                tokens().create(EDITOR_OWNER, slug, "lifecycle", new TokenOptions(TokenScope.WRITE, null));

        // when
        Optional<TokenIdentity> write = tokens().authenticate(created.secret());
        spaces().changeRole(ADMIN, slug, EDITOR.subject(), SpaceRole.VIEWER);
        Optional<TokenIdentity> read = tokens().authenticate(created.secret());
        spaces().removeMember(ADMIN, slug, EDITOR.subject());
        Optional<TokenIdentity> removed = tokens().authenticate(created.secret());

        // then
        TokenScope writeScope = write.orElseThrow().scope();
        TokenScope readScope = read.orElseThrow().scope();
        assertThat(writeScope).isEqualTo(TokenScope.WRITE);
        assertThat(readScope).isEqualTo(TokenScope.READ);
        assertThat(removed).isEmpty();
    }

    @Test
    void nonMemberIsToldTheSpaceWasNotFound() {
        // given
        Space space = seedSpaceWithMembers("stranger");
        String slug = space.slug();
        TokenOwner stranger = new TokenOwner(uniqueSubject("stranger"));

        // when
        Throwable create = catchThrowable(
                () -> tokens().create(stranger, slug, "stranger", new TokenOptions(TokenScope.READ, null)));
        Throwable list = catchThrowable(() -> tokens().list(stranger, slug));
        Throwable revoke = catchThrowable(() -> tokens().revoke(stranger, slug, UUID.randomUUID()));

        // then
        assertThat(create).isInstanceOf(SpaceNotFoundException.class);
        assertThat(list).isInstanceOf(SpaceNotFoundException.class);
        assertThat(revoke).isInstanceOf(SpaceNotFoundException.class);
    }

    private ApiTokens tokens() {
        return bean(ApiTokens.class);
    }

    private SpaceService spaces() {
        return bean(SpaceService.class);
    }

    private Space seedSpaceWithMembers(String prefix) {
        SpaceService service = spaces();
        PutResult created = service.put(ADMIN, uniqueSlug(prefix), "Space " + prefix);
        Space space = created.space();
        service.addMember(ADMIN, space.slug(), EDITOR.subject(), SpaceRole.EDITOR);
        service.addMember(ADMIN, space.slug(), VIEWER.subject(), SpaceRole.VIEWER);
        return space;
    }

    private static String uniqueSlug(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String uniqueSubject(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            byte[] hashed = digest.digest(bytes);
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
