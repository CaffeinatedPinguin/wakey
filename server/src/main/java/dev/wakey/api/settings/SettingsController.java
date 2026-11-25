package dev.wakey.api.settings;

import dev.wakey.api.SpacePrincipals;
import dev.wakey.settings.SpaceSettings;
import dev.wakey.settings.TimeoutExceedsIntervalException;
import dev.wakey.spaces.SpaceAccessDeniedException;
import dev.wakey.spaces.SpaceService;
import dev.wakey.spaces.SpaceView;
import dev.wakey.web.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * The HTTP adapter for the space settings and the assigned plan. Access rules and plan bounds live
 * in the spaces and settings modules; this adapter resolves the effective plan name.
 */
@RestController
final class SettingsController {

    private static final String COLLECTION_PATH = ApiPaths.API_V1 + "/spaces/{slug}/settings";

    private static final String ITEM_PATH = COLLECTION_PATH + "/{key}";

    private static final String PLAN_PATH = ApiPaths.API_V1 + "/spaces/{slug}/plan";

    private static final String JSON = "application/json";

    private static final String PROBLEM_JSON = "application/problem+json";

    private static final String WRITE_DENIED = "Only an editor or an administrator can change settings";

    private final SpaceService spaces;

    private final SpaceSettings settings;

    SettingsController(SpaceService spaces, SpaceSettings settings) {
        this.spaces = spaces;
        this.settings = settings;
    }

    @Operation(operationId = "getSpaceSettings", summary = "Read the effective settings of a space")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "400",
            description = "The space plan or a setting is invalid.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "The space was not found.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping(path = COLLECTION_PATH, produces = JSON)
    List<SpaceSettingResponse> getSpaceSettings(@PathVariable String slug, Authentication authentication) {
        var actor = SpacePrincipals.forSpace(authentication, slug);
        var view = spaces.view(actor, slug);
        String planName = settings.resolvePlanName(view.planName());
        var effective = settings.effectiveAll(view.id(), planName);
        List<SpaceSettingResponse> response = new ArrayList<>();
        for (var entry : effective.entrySet()) {
            String key = entry.getKey();
            var setting = entry.getValue();
            response.add(SpaceSettingResponse.from(key, setting, planName));
        }
        return response;
    }

    @Operation(operationId = "putSpaceSetting", summary = "Set a space setting within the plan")
    @ApiResponse(responseCode = "204", description = "The setting was stored.")
    @ApiResponse(
            responseCode = "400",
            description = "The value is outside the plan or has an invalid format.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "The caller cannot change the settings of this space.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "The space was not found.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping(path = ITEM_PATH, consumes = JSON, produces = JSON)
    ResponseEntity<Void> putSpaceSetting(
            @PathVariable String slug,
            @PathVariable String key,
            @Valid @RequestBody SetSpaceSettingRequest request,
            Authentication authentication) {
        var actor = SpacePrincipals.forSpace(authentication, slug);
        var view = spaces.view(actor, slug);
        requireWriter(view);
        try {
            settings.set(view.id(), view.planName(), key, request.value());
        } catch (TimeoutExceedsIntervalException exception) {
            throw SettingProblems.timeoutExceedsInterval(exception);
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "deleteSpaceSetting", summary = "Reset a space setting to the plan value")
    @ApiResponse(responseCode = "204", description = "The override was removed.")
    @ApiResponse(
            responseCode = "400",
            description = "The setting key is unknown.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "The caller cannot change the settings of this space.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "The space was not found.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping(path = ITEM_PATH)
    ResponseEntity<Void> deleteSpaceSetting(
            @PathVariable String slug, @PathVariable String key, Authentication authentication) {
        var actor = SpacePrincipals.forSpace(authentication, slug);
        var view = spaces.view(actor, slug);
        requireWriter(view);
        settings.reset(view.id(), key);
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "putSpacePlan", summary = "Assign a plan to a space idempotently")
    @ApiResponse(responseCode = "204", description = "The plan was assigned.")
    @ApiResponse(
            responseCode = "400",
            description = "The plan is not configured on this instance.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Only an administrator can assign a plan.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "The space was not found.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping(path = PLAN_PATH, consumes = JSON, produces = JSON)
    ResponseEntity<Void> putSpacePlan(
            @PathVariable String slug, @Valid @RequestBody PutSpacePlanRequest request, Authentication authentication) {
        var actor = SpacePrincipals.forSpace(authentication, slug);
        String planName = settings.requireExisting(request.plan());
        spaces.assignPlan(actor, slug, planName);
        return ResponseEntity.noContent().build();
    }

    private static void requireWriter(SpaceView view) {
        if (!view.canWrite()) {
            throw new SpaceAccessDeniedException(WRITE_DENIED);
        }
    }
}
