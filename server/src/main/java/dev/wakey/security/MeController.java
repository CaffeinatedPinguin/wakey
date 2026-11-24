package dev.wakey.security;

import dev.wakey.web.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
final class MeController {

    private static final String ME_PATH = ApiPaths.API_V1 + "/me";

    @Operation(operationId = "getMe", summary = "Return the authenticated user")
    @GetMapping(ME_PATH)
    MeResponse me(Authentication authentication) {
        boolean admin = AdminAuthoritiesMapper.isAdmin(authentication);
        return new MeResponse(authentication.getName(), admin);
    }

    record MeResponse(
            @Schema(description = "The OIDC subject of the authenticated user")
            String subject,

            @Schema(description = "Whether the user is an instance administrator")
            boolean admin) {}
}
