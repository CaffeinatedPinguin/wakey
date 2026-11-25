package dev.wakey.security;

import dev.wakey.web.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
final class MeController {

    private static final String ME_PATH = ApiPaths.API_V1 + "/me";

    private static final String JSON = "application/json";

    private static final String PROBLEM_JSON = "application/problem+json";

    @Operation(operationId = "getMe", summary = "Return the authenticated user")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping(path = ME_PATH, produces = JSON)
    CurrentUser me(Authentication authentication) {
        return CurrentUser.from(authentication);
    }
}
