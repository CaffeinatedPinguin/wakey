package dev.wakey.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
final class MeController {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    @GetMapping("/api/v1/me")
    MeResponse me(Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLE_ADMIN::equals);
        return new MeResponse(authentication.getName(), admin);
    }

    record MeResponse(String subject, boolean admin) {}
}
