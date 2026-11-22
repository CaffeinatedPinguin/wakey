package dev.wakey.security;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
final class MeController {

    @GetMapping("/api/v1/me")
    MeResponse me(Authentication authentication) {
        boolean admin = AdminAuthoritiesMapper.isAdmin(authentication);
        return new MeResponse(authentication.getName(), admin);
    }

    record MeResponse(String subject, boolean admin) {}
}
