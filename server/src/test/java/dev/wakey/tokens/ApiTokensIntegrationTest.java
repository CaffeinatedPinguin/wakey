package dev.wakey.tokens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import dev.wakey.spaces.PutResult;
import dev.wakey.spaces.Space;
import dev.wakey.spaces.SpaceError;
import dev.wakey.spaces.SpaceMember;
import dev.wakey.spaces.SpaceMemberId;
import dev.wakey.spaces.SpaceMembers;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceService;
import dev.wakey.testsupport.PostgresIntegrationTest;
import dev.wakey.web.ApiException;
import dev.wakey.web.ApiFieldError;
import dev.wakey.web.CommonError;
import dev.wakey.web.CurrentUser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class ApiTokensIntegrationTest extends PostgresIntegrationTest {

    private static final String ISSUER = "https://issuer.example.com";

    private static final CurrentUser ADMIN = new CurrentUser(ISSUER, "fixture-admin", true);

    private static final CurrentUser EDITOR = new CurrentUser(ISSUER, "fixture-editor", false);

    private static final CurrentUser VIEWER = new CurrentUser(ISSUER, "fixture-viewer", false);

    private static final TokenOwner EDITOR_OWNER = new TokenOwner(ISSUER, EDITOR.subject());

    private static final TokenOwner VIEWER_OWNER = new TokenOwner(ISSUER, VIEWER.subject());

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
        CreatedToken created = tokens().create(EDITOR_OWNER, slug, "expired", new TokenOptions(TokenScope.READ, null));
        UUID tokenId = created.token().id();
        expireAt(tokenId, expiredAt);

        // when
        Optional<TokenIdentity> found = tokens().authenticate(created.secret());

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void lastUsedIsNotWrittenOnEveryRequest() {
        // given
        Space space = seedSpaceWithMembers("last-used-threshold");
        String slug = space.slug();
        CreatedToken created = tokens().create(EDITOR_OWNER, slug, "used", new TokenOptions(TokenScope.READ, null));
        UUID tokenId = created.token().id();
        tokens().authenticate(created.secret());
        Instant firstUse = lastUsedAt(tokenId);

        // when
        tokens().authenticate(created.secret());

        // then
        Instant secondUse = lastUsedAt(tokenId);
        assertThat(firstUse).isNotNull();
        assertThat(secondUse).isEqualTo(firstUse);
    }

    @Test
    void removingAMemberRevokesTheirTokensInThatSpace() {
        // given
        Space revokedSpace = seedSpaceWithMembers("revoke-in");
        Space otherSpace = seedSpaceWithMembers("revoke-out");
        CreatedToken revokedToken =
                tokens().create(EDITOR_OWNER, revokedSpace.slug(), "revoked", new TokenOptions(TokenScope.WRITE, null));
        CreatedToken otherSpaceToken =
                tokens().create(EDITOR_OWNER, otherSpace.slug(), "other", new TokenOptions(TokenScope.WRITE, null));
        CreatedToken otherMemberToken =
                tokens().create(VIEWER_OWNER, revokedSpace.slug(), "viewer", new TokenOptions(TokenScope.READ, null));

        // when
        members().removeMember(ADMIN, revokedSpace.slug(), new SpaceMemberId(ISSUER, EDITOR.subject()));
        Optional<TokenIdentity> afterRemoval = tokens().authenticate(revokedToken.secret());
        Optional<TokenIdentity> otherSpaceStill = tokens().authenticate(otherSpaceToken.secret());
        Optional<TokenIdentity> otherMemberStill = tokens().authenticate(otherMemberToken.secret());

        // then
        assertThat(afterRemoval).isEmpty();
        assertThat(otherSpaceStill).isPresent();
        assertThat(otherMemberStill).isPresent();
    }

    @Test
    void parallelCreatesNeverExceedTheTokenLimit() throws Exception {
        // given
        Space space = seedSpaceWithMembers("parallel-limit");
        String slug = space.slug();
        for (int index = 0; index < 49; index++) {
            tokens().create(EDITOR_OWNER, slug, "token-" + index, new TokenOptions(TokenScope.READ, null));
        }
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Boolean> outcomes = new ArrayList<>();

        // when
        try {
            Callable<Boolean> create = () -> {
                start.await();
                return createsOneMoreToken(slug);
            };
            List<Future<Boolean>> futures = new ArrayList<>();
            futures.add(executor.submit(create));
            futures.add(executor.submit(create));
            start.countDown();
            for (Future<Boolean> future : futures) {
                outcomes.add(future.get());
            }
        } finally {
            executor.shutdown();
        }
        long stored = countStoredTokens(space.id(), EDITOR.subject());

        // then
        long created = outcomes.stream().filter(Boolean::booleanValue).count();
        assertThat(created).isEqualTo(1L);
        assertThat(stored).isEqualTo(50L);
    }

    @Test
    void tokenOwnersAreKeyedOnIssuerAndSubject() {
        // given
        String subject = uniqueSubject("shared-owner");
        String otherIssuer = "https://other-issuer.example.com";
        PutResult created = spaces().put(ADMIN, uniqueSlug("owner-issuers"), "Owner issuers", null);
        Space space = created.space();
        String slug = space.slug();
        members().addMember(ADMIN, slug, new SpaceMember(ISSUER, subject, SpaceRole.EDITOR));
        members().addMember(ADMIN, slug, new SpaceMember(otherIssuer, subject, SpaceRole.EDITOR));
        TokenOwner firstOwner = new TokenOwner(ISSUER, subject);
        TokenOwner secondOwner = new TokenOwner(otherIssuer, subject);
        for (int index = 0; index < 50; index++) {
            tokens().create(firstOwner, slug, "first-" + index, new TokenOptions(TokenScope.READ, null));
        }

        // when
        CreatedToken second = tokens().create(secondOwner, slug, "second", new TokenOptions(TokenScope.READ, null));
        List<TokenSummary> firstList = tokens().list(firstOwner, slug);
        List<TokenSummary> secondList = tokens().list(secondOwner, slug);

        // then
        TokenSummary secondToken = second.token();
        assertThat(secondToken.name()).isEqualTo("second");
        assertThat(firstList).hasSize(50);
        assertThat(secondList).extracting(TokenSummary::id).containsExactly(secondToken.id());
    }

    @Test
    void aRevokedTokenStaysListed() {
        // given
        Space space = seedSpaceWithMembers("revoked-listed");
        String slug = space.slug();
        CreatedToken created = tokens().create(EDITOR_OWNER, slug, "revoked", new TokenOptions(TokenScope.READ, null));
        UUID tokenId = created.token().id();
        tokens().revoke(EDITOR_OWNER, slug, tokenId);
        Instant longAgo = Instant.now(bean(Clock.class)).minus(Duration.ofDays(31));
        setRevokedAt(tokenId, longAgo);

        // when
        List<TokenSummary> listed = tokens().list(EDITOR_OWNER, slug);

        // then
        assertThat(listed).extracting(TokenSummary::id).contains(tokenId);
    }

    private boolean createsOneMoreToken(String slug) {
        try {
            tokens().create(EDITOR_OWNER, slug, "parallel", new TokenOptions(TokenScope.READ, null));
            return true;
        } catch (ApiException exception) {
            if (exception.getErrorCode() != TokenError.TOKEN_LIMIT) {
                throw exception;
            }
            return false;
        }
    }

    private long countStoredTokens(UUID spaceId, String subject) {
        return jdbc().sql("SELECT count(*) FROM api_token WHERE space_id = :spaceId AND owner_subject = :subject")
                .param("spaceId", spaceId)
                .param("subject", subject)
                .query(Long.class)
                .single();
    }

    @Test
    void theTokenIndexCoversSpaceAndOwner() {
        // when
        List<String> indexes = jdbc().sql("SELECT indexname FROM pg_indexes WHERE tablename = 'api_token'")
                .query((rs, rowNum) -> Objects.requireNonNull(rs.getString("indexname")))
                .list();
        List<String> definitions = jdbc().sql("SELECT indexdef FROM pg_indexes WHERE tablename = 'api_token'")
                .query((rs, rowNum) -> Objects.requireNonNull(rs.getString("indexdef")))
                .list();

        // then
        assertThat(indexes).doesNotContain("ix_api_token_owner_subject");
        assertThat(definitions).anyMatch(definition -> definition.contains("(space_id, owner_issuer, owner_subject)"));
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
    void aTokenNameWithAControlCharacterIsRejected() {
        // given
        Space space = seedSpaceWithMembers("control-name");
        String slug = space.slug();

        // when
        Throwable rejected = catchThrowable(
                () -> tokens().create(EDITOR_OWNER, slug, "bad\u0007name", new TokenOptions(TokenScope.READ, null)));

        // then
        assertThat(rejected).isInstanceOf(ApiException.class);
        ApiException exception = (ApiException) rejected;
        assertThat(exception.getErrorCode()).isEqualTo(CommonError.VALIDATION_FAILED);
        assertThat(exception.getFieldErrors()).hasSize(1);
        ApiFieldError problem = exception.getFieldErrors().getFirst();
        assertThat(problem.pointer()).isEqualTo("/name");
        assertThat(problem.code()).isEqualTo(SpaceError.INVALID_NAME.getCode());
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
        assertThat(denied).isInstanceOf(ApiException.class);
        assertThat(((ApiException) denied).getErrorCode()).isEqualTo(CommonError.FORBIDDEN);
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
        members().changeRole(ADMIN, slug, new SpaceMember(ISSUER, EDITOR.subject(), SpaceRole.VIEWER));
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
        members().removeMember(ADMIN, slug, new SpaceMemberId(ISSUER, EDITOR.subject()));
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
    void nonMemberIsToldTheSpaceWasNotFound() {
        // given
        Space space = seedSpaceWithMembers("stranger");
        String slug = space.slug();
        TokenOwner stranger = new TokenOwner(ISSUER, uniqueSubject("stranger"));

        // when
        Throwable create = catchThrowable(
                () -> tokens().create(stranger, slug, "stranger", new TokenOptions(TokenScope.READ, null)));
        Throwable list = catchThrowable(() -> tokens().list(stranger, slug));
        Throwable revoke = catchThrowable(() -> tokens().revoke(stranger, slug, UUID.randomUUID()));

        // then
        assertNotFound(create);
        assertNotFound(list);
        assertNotFound(revoke);
    }

    private static void assertNotFound(Throwable throwable) {
        assertThat(throwable).isInstanceOf(ApiException.class);
        assertThat(((ApiException) throwable).getErrorCode()).isEqualTo(CommonError.NOT_FOUND);
    }

    private void expireAt(UUID tokenId, Instant expiresAt) {
        OffsetDateTime value = OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC);
        jdbc().sql("UPDATE api_token SET expires_at = :expiresAt WHERE id = :id")
                .param("expiresAt", value)
                .param("id", tokenId)
                .update();
    }

    private void setRevokedAt(UUID tokenId, Instant revokedAt) {
        OffsetDateTime value = OffsetDateTime.ofInstant(revokedAt, ZoneOffset.UTC);
        jdbc().sql("UPDATE api_token SET revoked_at = :revokedAt WHERE id = :id")
                .param("revokedAt", value)
                .param("id", tokenId)
                .update();
    }

    private Instant lastUsedAt(UUID tokenId) {
        OffsetDateTime value = jdbc().sql("SELECT last_used_at FROM api_token WHERE id = :id")
                .param("id", tokenId)
                .query(OffsetDateTime.class)
                .single();
        return value.toInstant();
    }

    private ApiTokens tokens() {
        return bean(ApiTokens.class);
    }

    private SpaceService spaces() {
        return bean(SpaceService.class);
    }

    private SpaceMembers members() {
        return bean(SpaceMembers.class);
    }

    private Space seedSpaceWithMembers(String prefix) {
        SpaceService service = spaces();
        PutResult created = service.put(ADMIN, uniqueSlug(prefix), "Space " + prefix, null);
        Space space = created.space();
        members().addMember(ADMIN, space.slug(), new SpaceMember(ISSUER, EDITOR.subject(), SpaceRole.EDITOR));
        members().addMember(ADMIN, space.slug(), new SpaceMember(ISSUER, VIEWER.subject(), SpaceRole.VIEWER));
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
