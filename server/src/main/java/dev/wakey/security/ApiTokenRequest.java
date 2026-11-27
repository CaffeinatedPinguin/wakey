package dev.wakey.security;

import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;

/** An unauthenticated API token secret presented in an {@code Authorization: Bearer} header. */
final class ApiTokenRequest extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 1L;

    private final transient String secret;

    ApiTokenRequest(String secret) {
        super(List.of());
        this.secret = secret;
    }

    String secret() {
        return secret;
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return "";
    }
}
