package dev.wakey.api.spaces;

import dev.wakey.spaces.SpaceService;
import dev.wakey.spaces.SpaceView;
import dev.wakey.web.ApiProperties;
import dev.wakey.web.CursorPage;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The HTTP adapter for spaces; all access rules and validation live in the spaces module. */
@RestController
@Tag(name = "spaces")
@RequiredArgsConstructor
final class SpacesController {

    private final SpaceService spaces;

    private final ApiProperties apiProperties;

    @GetMapping(SpacesHttp.PATH)
    CursorPage<SpaceResponse> listSpaces(
            @Parameter(
                            description = "The page size, at least 1; values above the instance maximum are capped.",
                            schema = @Schema(type = "integer"))
                    @RequestParam(required = false)
                    @Positive
                    Integer limit,
            @RequestParam(required = false) @Parameter(schema = @Schema(type = "string")) String cursor) {
        var actor = SpacePrincipals.current();
        int pageSize = apiProperties.pageSize(limit);
        String afterSlug = SpacesHttp.afterSlug(cursor);
        var rows = spaces.listVisible(actor, afterSlug, pageSize + 1);
        return CursorPage.of(rows, pageSize, SpaceView::slug, SpaceResponse::from);
    }

    @GetMapping(path = SpacesHttp.PATH + "/{slug}", produces = "application/json")
    SpaceResponse getSpace(
            @PathVariable @Parameter(schema = @Schema(pattern = SpaceResponse.SLUG_PATTERN)) String slug) {
        var actor = SpacePrincipals.current();
        return SpaceResponse.from(spaces.view(actor, slug));
    }

    @CreateSpaceOperation
    ResponseEntity<SpaceResponse> createSpace(@Valid @RequestBody CreateSpaceRequest request) {
        var actor = SpacePrincipals.current();
        var result = spaces.create(actor, request.slug(), request.name());
        String storedSlug = result.space().slug();
        var view = spaces.view(actor, storedSlug);
        return ResponseEntity.created(SpacesHttp.location(storedSlug)).body(SpaceResponse.from(view));
    }

    @PutSpaceOperation
    ResponseEntity<SpaceResponse> putSpace(
            @PathVariable @Parameter(schema = @Schema(pattern = SpaceResponse.SLUG_PATTERN)) String slug,
            @Valid @RequestBody PutSpaceRequest request) {
        var actor = SpacePrincipals.current();
        var result = spaces.put(actor, slug, request.name(), request.version());
        String storedSlug = result.space().slug();
        var view = spaces.view(actor, storedSlug);
        if (result.created()) {
            return ResponseEntity.created(SpacesHttp.location(storedSlug)).body(SpaceResponse.from(view));
        }
        return ResponseEntity.ok(SpaceResponse.from(view));
    }
}
