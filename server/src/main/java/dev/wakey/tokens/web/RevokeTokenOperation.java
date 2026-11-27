package dev.wakey.tokens.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/** Maps the token revocation operation and documents all of its answers together. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Operation(operationId = "revokeToken", summary = "Revoke one of the caller's API tokens")
@ApiResponses({
    @ApiResponse(responseCode = "204", description = "The token was revoked."),
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
            responseCode = "403",
            description = "The caller is not allowed to revoke this token.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
            responseCode = "404",
            description = "The space was not found.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
})
@RequestMapping(path = TokenPaths.ITEM, method = RequestMethod.DELETE)
@interface RevokeTokenOperation {}
