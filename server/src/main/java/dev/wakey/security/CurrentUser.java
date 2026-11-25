package dev.wakey.security;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.core.Authentication;

/** The authenticated caller as the HTTP adapters see it: the OIDC subject and the application Admin flag. */
public record CurrentUser(
        @Schema(description = "The OIDC subject of the authenticated user", requiredMode = Schema.RequiredMode.REQUIRED)
        String subject,

        @Schema(
                description = "Whether the user is an instance administrator",
                requiredMode = Schema.RequiredMode.REQUIRED)
        boolean admin) {

    public static CurrentUser from(Authentication authentication) {
        return new CurrentUser(authentication.getName(), AdminAuthoritiesMapper.isAdmin(authentication));
    }
}
