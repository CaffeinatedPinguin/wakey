package dev.wakey.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

/** Adds {@code ROLE_ADMIN} when the configured claim contains the configured Admin group. */
final class AdminAuthoritiesMapper implements GrantedAuthoritiesMapper {

    private static final GrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

    private final String adminClaim;

    private final String adminGroup;

    AdminAuthoritiesMapper(String adminClaim, String adminGroup) {
        this.adminClaim = adminClaim;
        this.adminGroup = adminGroup;
    }

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Set<GrantedAuthority> mapped = new LinkedHashSet<>(authorities);
        if (!adminGroup.isBlank() && hasAdminGroup(authorities)) {
            mapped.add(ADMIN);
        }
        return mapped;
    }

    private boolean hasAdminGroup(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .filter(OidcUserAuthority.class::isInstance)
                .map(OidcUserAuthority.class::cast)
                .anyMatch(this::containsAdminGroup);
    }

    private boolean containsAdminGroup(OidcUserAuthority authority) {
        Object claim = authority.getAttributes().get(adminClaim);
        return claim instanceof Collection<?> groups && groups.contains(adminGroup);
    }
}
