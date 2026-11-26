package dev.wakey.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

class CurrentUserTest {

    @Test
    void theCurrentUserTakesIssuerAndSubjectFromTheOidcToken() {
        // given
        Map<String, Object> claims = Map.of("sub", "subject-1", "iss", "https://issuer.example.com");
        Instant expiresAt = Instant.EPOCH.plusSeconds(600);
        OidcIdToken idToken = new OidcIdToken("id-token", Instant.EPOCH, expiresAt, claims);
        OidcUserInfo userInfo = new OidcUserInfo(Map.of("sub", "subject-1", "email", "user@example.com"));
        OidcUserAuthority authority = new OidcUserAuthority(idToken);
        OidcUser oidcUser = new DefaultOidcUser(List.of(authority), idToken, userInfo, "email");
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(oidcUser, "n/a", List.of(authority));

        // when
        CurrentUser currentUser = CurrentUser.from(authentication);

        // then
        assertThat(authentication.getName()).isEqualTo("user@example.com");
        assertThat(currentUser.issuer()).isEqualTo("https://issuer.example.com");
        assertThat(currentUser.subject()).isEqualTo("subject-1");
        assertThat(currentUser.admin()).isFalse();
    }
}
