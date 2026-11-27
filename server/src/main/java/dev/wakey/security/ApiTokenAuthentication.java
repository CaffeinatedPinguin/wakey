package dev.wakey.security;

import dev.wakey.tokens.TokenIdentity;
import dev.wakey.tokens.TokenScope;
import dev.wakey.web.TokenScopedAuthentication;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** The authentication of a request that presented a valid {@code wk_} token. */
public final class ApiTokenAuthentication extends TokenScopedAuthentication {

    private static final long serialVersionUID = 1L;

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final transient TokenIdentity identity;

    ApiTokenAuthentication(TokenIdentity identity) {
        super(identity.spaceSlug(), identity.ownerIssuer(), identity.ownerSubject(), authorities(identity));
        this.identity = identity;
    }

    TokenIdentity identity() {
        return identity;
    }

    /** True when this is a read token trying to write; it is the insufficient-scope case. */
    boolean deniesWrite(String method) {
        return identity.scope() == TokenScope.READ && WRITE_METHODS.contains(method);
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
