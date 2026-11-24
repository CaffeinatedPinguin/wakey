package dev.wakey.api.spaces;

import dev.wakey.spaces.SpaceService;
import dev.wakey.web.ApiPaths;
import dev.wakey.web.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The HTTP adapter for spaces; all access rules and validation live in the spaces module. */
@RestController
final class SpacesController {

    private static final String COLLECTION_PATH = ApiPaths.API_V1 + "/spaces";

    private static final String ITEM_PATH = COLLECTION_PATH + "/{slug}";

    private static final String DEFAULT_PAGE_SIZE = "20";

    private static final int CREATED_STATUS = 201;

    private static final int OK_STATUS = 200;

    private final SpaceService spaces;

    private final SpacesSupport support;

    SpacesController(SpaceService spaces, SpacesSupport support) {
        this.spaces = spaces;
        this.support = support;
    }

    @Operation(operationId = "listSpaces", summary = "List the spaces visible to the caller")
    @GetMapping(COLLECTION_PATH)
    CursorPage<SpaceResponse> listSpaces(
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int limit,
            @RequestParam(required = false) Optional<String> cursor,
            Authentication authentication) {
        var actor = support.principal(authentication);
        return support.page(actor, limit, cursor);
    }

    @Operation(operationId = "getSpace", summary = "Read one space visible to the caller")
    @GetMapping(ITEM_PATH)
    SpaceResponse getSpace(@PathVariable String slug, Authentication authentication) {
        var actor = support.principal(authentication);
        return support.find(actor, slug);
    }

    @Operation(operationId = "putSpace", summary = "Create or rename a space idempotently")
    @PutMapping(ITEM_PATH)
    ResponseEntity<SpaceResponse> putSpace(
            @PathVariable String slug, @Valid @RequestBody PutSpaceRequest request, Authentication authentication) {
        var actor = support.principal(authentication);
        var result = spaces.put(actor, slug, request.name());
        String createdSlug = result.space().slug();
        var response = support.find(actor, createdSlug);
        int status = result.created() ? CREATED_STATUS : OK_STATUS;
        return ResponseEntity.status(status).body(response);
    }
}
