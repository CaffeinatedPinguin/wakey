package dev.wakey.settings.web;

import dev.wakey.settings.SettingValue;
import dev.wakey.spaces.web.SpaceReader;
import dev.wakey.spaces.web.SpaceWriter;
import dev.wakey.web.ApiPaths;
import dev.wakey.web.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * The HTTP adapter for the space settings and the assigned plan. Access rules and the plan of a
 * space live in the spaces and settings modules; the settings module resolves the plan of the space.
 */
@RestController
@RequiredArgsConstructor
final class SettingsController {

    private static final String COLLECTION_PATH = ApiPaths.API_V1 + "/spaces/{slug}/settings";

    private static final String ITEM_PATH = COLLECTION_PATH + "/{key}";

    private static final String PLAN_PATH = ApiPaths.API_V1 + "/spaces/{slug}/plan";

    private final SettingsResources resources;

    @Operation(operationId = "getSpaceSettings", summary = "Read the effective settings of a space")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @GetMapping(path = COLLECTION_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    CursorPage<SpaceSettingResponse> getSpaceSettings(@PathVariable String slug, SpaceReader reader) {
        return resources.list(reader.space());
    }

    @Operation(operationId = "putSpaceSetting", summary = "Set a space setting within the plan")
    @ApiResponse(responseCode = "204", description = "The setting was stored.")
    @PutMapping(
            path = ITEM_PATH,
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
    @DeleteMapping(path = COLLECTION_PATH)
    ResponseEntity<Void> resetSpaceSettings(@PathVariable String slug, SpaceWriter writer) {
        resources.resetAll(writer.user(), writer.space(), writer.viaToken());
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "deleteSpaceSetting", summary = "Reset a space setting to the plan value")
    @ApiResponse(responseCode = "204", description = "The override was removed.")
    @DeleteMapping(path = ITEM_PATH)
    ResponseEntity<Void> deleteSpaceSetting(@PathVariable String slug, @PathVariable String key, SpaceWriter writer) {
        resources.reset(writer.user(), writer.space(), key, writer.viaToken());
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "putSpacePlan", summary = "Assign a plan to a space idempotently")
    @ApiResponse(responseCode = "204", description = "The plan was assigned.")
    @PutMapping(
            path = PLAN_PATH,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> putSpacePlan(
            @PathVariable String slug, SpaceWriter writer, @Valid @RequestBody PutSpacePlanRequest request) {
        resources.assign(writer.user(), slug, request.plan(), writer.viaToken());
        return ResponseEntity.noContent().build();
    }
}
