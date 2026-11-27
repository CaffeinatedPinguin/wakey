package dev.wakey.security.web;

import dev.wakey.web.ApiPaths;
import dev.wakey.web.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
final class MeController {

    private static final String ME_PATH = ApiPaths.API_V1 + "/me";

    @Operation(operationId = "getMe", summary = "Return the authenticated user")
    @GetMapping(path = ME_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    MeResponse me(CurrentUser user) {
        return new MeResponse(user.issuer(), user.subject(), user.admin());
    }

    record MeResponse(
            @Schema(
                    description = "The OIDC issuer of the authenticated user",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            String issuer,

            @Schema(
                    description = "The OIDC subject of the authenticated user",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            String subject,

            @Schema(
                    description = "Whether the user is an instance administrator",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            boolean admin) {}
}
