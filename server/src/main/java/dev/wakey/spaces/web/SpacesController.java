package dev.wakey.spaces.web;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The HTTP adapter for spaces; all access rules and validation live in the spaces module. */
@RestController
@Tag(name = "spaces")
@RequiredArgsConstructor
final class SpacesController {

    private final SpacesResources resources;

    @GetMapping(path = SpacesHttp.PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    CursorPage<SpaceResponse> listSpaces(
            @RequestParam(required = false) @Positive Integer limit,
            @RequestParam(required = false) String cursor,
            SpaceCaller caller) {
        return resources.list(caller, limit, cursor);
    }

    @GetMapping(path = SpacesHttp.PATH + "/{slug}", produces = MediaType.APPLICATION_JSON_VALUE)
    SpaceResponse getSpace(@PathVariable String slug, SpaceReader reader) {
        return resources.get(reader);
    }

    @CreateSpaceOperation
    ResponseEntity<SpaceResponse> createSpace(SpaceCaller caller, @Valid @RequestBody CreateSpaceRequest request) {
        return resources.create(caller, request);
    }

    @PutSpaceOperation
    ResponseEntity<SpaceResponse> putSpace(
            @PathVariable String slug, SpaceUpsert upsert, @Valid @RequestBody PutSpaceRequest request) {
        return resources.put(slug, upsert, request);
    }
}
