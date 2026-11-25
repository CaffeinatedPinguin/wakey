package dev.wakey.api.spaces;

import dev.wakey.spaces.PutResult;
import dev.wakey.spaces.SpaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** The HTTP write adapter for spaces; all access rules and validation live in the spaces module. */
@RestController
@RequiredArgsConstructor
final class SpacesPutController {

    private final SpaceService spaces;

    @Operation(
            operationId = "putSpace",
            summary = "Create or rename a space idempotently",
            description = "A rename is last-write-wins unless an If-Match precondition is sent. "
                    + "Send If-None-Match: * to create the space only when the slug is free, and "
                    + "If-Match: \"<version>\" to rename only the version the caller read.")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "400",
            description = "The request is invalid or a precondition header is malformed.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    @ApiResponse(
            responseCode = "403",
            description = "The caller is not allowed to create or rename spaces.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    @ApiResponse(
            responseCode = "409",
            description = "The slug is taken or the If-Match version is stale.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    @ApiResponse(
            responseCode = "201",
            description = "The space was created.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SpaceResponse.class)))
    @PutMapping(path = SpacesPaths.ITEM, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SpaceResponse> putSpace(
            @PathVariable @Parameter(schema = @Schema(pattern = SpaceResponse.SLUG_PATTERN)) String slug,
            HttpHeaders headers,
            @Valid @RequestBody PutSpaceRequest request,
            Authentication authentication) {
        var actor = SpacePrincipals.of(authentication);
        boolean createOnly = SpacesPreconditions.isCreateOnly(headers);
        @Nullable Long expectedVersion = createOnly ? null : SpacesPreconditions.version(headers);
        PutResult result = createOnly
                ? spaces.create(actor, slug, request.name())
                : spaces.put(actor, slug, request.name(), expectedVersion);
        String storedSlug = result.space().slug();
        var view = spaces.view(actor, storedSlug);
        ResponseEntity.BodyBuilder response = ResponseEntity.status(result.created() ? 201 : 200);
        if (result.created()) {
            response = response.header("Location", SpacesPaths.COLLECTION + "/" + storedSlug);
        }
        return response.body(SpaceResponse.from(view));
    }
}
