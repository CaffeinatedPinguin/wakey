package dev.wakey.web;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/** An authentication bound to exactly one space and the token owner's identity. */
public abstract class TokenScopedAuthentication extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 1L;

    private final String spaceSlug;

    private final String ownerIssuer;

    private final String ownerSubject;

    protected TokenScopedAuthentication(
            String spaceSlug,
            String ownerIssuer,
            String ownerSubject,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.spaceSlug = spaceSlug;
        this.ownerIssuer = ownerIssuer;
        this.ownerSubject = ownerSubject;
        setAuthenticated(true);
    }

    public final String tokenSpaceSlug() {
        return spaceSlug;
    }

    public final String ownerIssuer() {
        return ownerIssuer;
    }

    public final String ownerSubject() {
        return ownerSubject;
    }
}
