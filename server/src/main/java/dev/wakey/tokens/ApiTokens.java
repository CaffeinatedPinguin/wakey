package dev.wakey.tokens;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The module's public port. A token belongs to one space and one owner, and its effective scope
 * never exceeds the owner's current role in that space. The HTTP layer maps the domain exceptions
 * to Problem Details; a caller that cannot see the space gets the spaces module's not-found
 * exception.
 */
public interface ApiTokens {

    /**
     * Creates a token and returns its secret once; only the SHA-256 hash is stored. The secret is
     * never part of the {@link CreatedToken#toString()}. A viewer may only create a read token.
     */
    CreatedToken create(TokenOwner owner, String spaceSlug, String name, TokenOptions options);

    /**
     * Resolves a presented secret to its token, or an empty result for a wrong prefix, an unknown,
     * expired or revoked token, or an owner that is no longer a member of the space. A successful
     * call records the use.
     */
    Optional<TokenIdentity> authenticate(String presentedSecret);

    /** Lists the owner's tokens in a space; the result never carries a secret or a hash. */
    List<TokenSummary> list(TokenOwner owner, String spaceSlug);

    /** Revokes the owner's token in a space; revoking an already revoked token changes nothing. */
    void revoke(TokenOwner owner, String spaceSlug, UUID tokenId);
}
