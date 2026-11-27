package dev.wakey.spaces.web;

import dev.wakey.security.CurrentUser;
import dev.wakey.spaces.SpaceService;
import dev.wakey.web.ApiProperties;
import dev.wakey.web.CursorPage;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The HTTP adapter for spaces; all access rules and validation live in the spaces module. */
@RestController
@RequestMapping(SpacesHttp.PATH)
@Tag(name = "spaces")
@RequiredArgsConstructor
final class SpacesController {

    private final SpaceService spaces;

    private final ApiProperties apiProperties;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    CursorPage<SpaceResponse> listSpaces(
            @RequestParam(required = false) @Positive Integer limit,
            @RequestParam(required = false) String cursor,
            CurrentUser actor) {
        int pageSize = apiProperties.pageSize(limit);
        String afterSlug = SpacesHttp.afterSlug(cursor);
        var rows = spaces.listVisible(actor, afterSlug, pageSize + 1);
        return CursorPage.of(rows, pageSize, view -> view.slug(), SpaceResponse::from);
    }

    @GetMapping(path = "/{slug}", produces = MediaType.APPLICATION_JSON_VALUE)
    SpaceResponse getSpace(@PathVariable String slug, CurrentUser actor) {
        return SpaceResponse.from(spaces.view(actor, slug));
    }

    @CreateSpaceOperation
    ResponseEntity<SpaceResponse> createSpace(@Valid @RequestBody CreateSpaceRequest request, CurrentUser actor) {
        var result = spaces.create(actor, request.slug(), request.name());
        String storedSlug = result.space().slug();
        var view = spaces.view(actor, storedSlug);
        return ResponseEntity.created(SpacesHttp.location(storedSlug)).body(SpaceResponse.from(view));
    }

    @PutSpaceOperation
    ResponseEntity<SpaceResponse> putSpace(
            @PathVariable String slug, @Valid @RequestBody PutSpaceRequest request, CurrentUser actor) {
        var result = spaces.put(actor, slug, request.name(), request.version());
        String storedSlug = result.space().slug();
        var view = spaces.view(actor, storedSlug);
        if (result.created()) {
            return ResponseEntity.created(SpacesHttp.location(storedSlug)).body(SpaceResponse.from(view));
        }
        return ResponseEntity.ok(SpaceResponse.from(view));
    }
}
