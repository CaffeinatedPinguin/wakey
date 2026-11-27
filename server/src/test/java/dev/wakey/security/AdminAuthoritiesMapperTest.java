package dev.wakey.security;

import static org.assertj.core.api.Assertions.assertThat;

import dev.wakey.testsupport.PostgresIntegrationTest;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

class AdminAuthoritiesMapperTest extends PostgresIntegrationTest {

    @Test
    void configuredAdminGroupAddsAdminRole() {
        // given
        GrantedAuthoritiesMapper mapper = bean(GrantedAuthoritiesMapper.class);
        OidcUserAuthority authority = oidcAuthority("wakey-admins");

        // when
        Collection<? extends GrantedAuthority> mapped = mapper.mapAuthorities(List.of(authority));

        // then
        List<String> roles = mapped.stream().map(GrantedAuthority::getAuthority).toList();
        assertThat(roles).contains("ROLE_ADMIN");
    }

    @Test
    void otherGroupDoesNotAddAdminRole() {
        // given
        GrantedAuthoritiesMapper mapper = bean(GrantedAuthoritiesMapper.class);
        OidcUserAuthority authority = oidcAuthority("other");

        // when
        Collection<? extends GrantedAuthority> mapped = mapper.mapAuthorities(List.of(authority));

        // then
        List<String> roles = mapped.stream().map(GrantedAuthority::getAuthority).toList();
        assertThat(roles).doesNotContain("ROLE_ADMIN");
    }

    private static OidcUserAuthority oidcAuthority(String group) {
        Map<String, Object> claims = Map.of("sub", "alice", "groups", List.of(group));
        OidcIdToken idToken = new OidcIdToken("id-token", Instant.EPOCH, Instant.EPOCH.plusSeconds(600), claims);
        return new OidcUserAuthority(idToken);
    }
}
