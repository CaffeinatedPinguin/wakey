package dev.wakey.security;

import dev.wakey.web.ApiPaths;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
final class MeController {

    private static final String ME_PATH = ApiPaths.API_V1 + "/me";

    @GetMapping(ME_PATH)
    MeResponse me(Authentication authentication) {
        boolean admin = AdminAuthoritiesMapper.isAdmin(authentication);
        return new MeResponse(authentication.getName(), admin);
    }

    record MeResponse(String subject, boolean admin) {}
}
