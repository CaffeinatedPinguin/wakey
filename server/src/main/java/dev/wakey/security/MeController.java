package dev.wakey.security;

import dev.wakey.web.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
final class MeController {

    private static final String ME_PATH = ApiPaths.API_V1 + "/me";

    @Operation(operationId = "getMe", summary = "Return the authenticated user")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @GetMapping(path = ME_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    CurrentUser me(Authentication authentication) {
        return CurrentUser.from(authentication);
    }
}
