package dev.wakey.security;

import dev.wakey.tokens.TokenIdentity;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** The authentication of a request that presented a valid {@code wk_} token. */
final class ApiTokenAuthentication extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 1L;

    private final transient TokenIdentity identity;

    ApiTokenAuthentication(TokenIdentity identity) {
        super(authorities(identity));
        this.identity = identity;
        setAuthenticated(true);
    }

    TokenIdentity identity() {
        return identity;
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return identity.ownerSubject();
    }

    private static List<GrantedAuthority> authorities(TokenIdentity identity) {
        String scope = "SCOPE_" + identity.scope().name();
        GrantedAuthority authority = new SimpleGrantedAuthority(scope);
        return List.of(authority);
    }
}
