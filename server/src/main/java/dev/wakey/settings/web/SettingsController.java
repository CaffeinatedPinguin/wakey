package dev.wakey.settings.web;

import dev.wakey.settings.SettingValue;
import dev.wakey.spaces.web.SpaceReader;
import dev.wakey.spaces.web.SpaceWriter;
import dev.wakey.web.ApiPaths;
import dev.wakey.web.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * The HTTP adapter for the space settings and the assigned plan. Access rules and the plan of a
 * space live in the spaces and settings modules; the settings module resolves the plan of the space.
 */
@RestController
@Tag(name = "settings")
@RequiredArgsConstructor
final class SettingsController {

    private static final String COLLECTION_PATH = ApiPaths.API_V1 + "/spaces/{slug}/settings";

    private static final String ITEM_PATH = COLLECTION_PATH + "/{key}";

    private static final String PLAN_PATH = ApiPaths.API_V1 + "/spaces/{slug}/plan";

    private static final String FORBIDDEN = "The caller cannot change the settings of this space.";

    private final SettingsResources resources;

    @Operation(operationId = "getSpaceSettings", summary = "Read the effective settings of a space")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "400",
            description = "The space plan or a setting is invalid.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @RequestMapping(path = COLLECTION_PATH, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    CursorPage<SpaceSettingResponse> getSpaceSettings(@PathVariable String slug, SpaceReader reader) {
        return resources.list(reader.space());
    }

    @Operation(operationId = "putSpaceSetting", summary = "Set a space setting within the plan")
    @ApiResponse(responseCode = "204", description = "The setting was stored.")
    @ApiResponse(
            responseCode = "400",
            description = "The value is outside the plan or has an invalid format.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = FORBIDDEN,
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @RequestMapping(
            path = ITEM_PATH,
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> putSpaceSetting(
            @PathVariable String slug,
            @PathVariable String key,
            SpaceWriter writer,
            @Valid @RequestBody SetSpaceSettingRequest request) {
        SettingValue value = new SettingValue(key, request.value());
        resources.set(writer.user(), writer.space(), value, writer.viaToken());
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "resetSpaceSettings", summary = "Reset every space setting to the plan values")
    @ApiResponse(responseCode = "204", description = "The overrides were removed.")
    @ApiResponse(
            responseCode = "403",
            description = FORBIDDEN,
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @RequestMapping(path = COLLECTION_PATH, method = RequestMethod.DELETE)
    ResponseEntity<Void> resetSpaceSettings(@PathVariable String slug, SpaceWriter writer) {
        resources.resetAll(writer.user(), writer.space(), writer.viaToken());
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "deleteSpaceSetting", summary = "Reset a space setting to the plan value")
    @ApiResponse(responseCode = "204", description = "The override was removed.")
    @ApiResponse(
            responseCode = "400",
            description = "The setting key is unknown.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = FORBIDDEN,
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @RequestMapping(path = ITEM_PATH, method = RequestMethod.DELETE)
    ResponseEntity<Void> deleteSpaceSetting(@PathVariable String slug, @PathVariable String key, SpaceWriter writer) {
        resources.reset(writer.user(), writer.space(), key, writer.viaToken());
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "putSpacePlan", summary = "Assign a plan to a space idempotently")
    @ApiResponse(responseCode = "204", description = "The plan was assigned.")
    @ApiResponse(
            responseCode = "400",
            description = "The requested plan is not configured on this instance.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Only an administrator can assign a plan.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @RequestMapping(
            path = PLAN_PATH,
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> putSpacePlan(
            @PathVariable String slug, SpaceWriter writer, @Valid @RequestBody PutSpacePlanRequest request) {
        resources.assign(writer.user(), slug, request.plan(), writer.viaToken());
        return ResponseEntity.noContent().build();
    }
}
