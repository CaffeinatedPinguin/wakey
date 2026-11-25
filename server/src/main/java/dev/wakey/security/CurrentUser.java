package dev.wakey.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;

/**
 * The authenticated caller as the HTTP adapters see it: the OIDC subject, the application Admin
 * flag and, for an API token, the one space the token may reach.
 */
public record CurrentUser(
        @Schema(description = "The OIDC subject of the authenticated user", requiredMode = Schema.RequiredMode.REQUIRED)
        String subject,

        @Schema(
                description = "Whether the user is an instance administrator",
                requiredMode = Schema.RequiredMode.REQUIRED)
        boolean admin,

        @Schema(hidden = true) @JsonIgnore @Nullable String tokenSpaceSlug) {

    public static CurrentUser from(Authentication authentication) {
        return new CurrentUser(
                authentication.getName(),
                AdminAuthoritiesMapper.isAdmin(authentication),
                tokenSpaceSlugOf(authentication));
    }

    private static @Nullable String tokenSpaceSlugOf(Authentication authentication) {
        @Nullable String slug = null;
        if (authentication instanceof ApiTokenAuthentication tokenAuthentication) {
            slug = tokenAuthentication.identity().spaceSlug();
        }
        return slug;
    }
}
