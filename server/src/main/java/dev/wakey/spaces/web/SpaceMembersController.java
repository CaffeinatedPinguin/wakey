package dev.wakey.spaces.web;

import dev.wakey.security.CurrentUser;
import dev.wakey.spaces.SpaceService;
import dev.wakey.web.ApiProperties;
import dev.wakey.web.CursorPage;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The HTTP adapter for listing and adding the memberships of a space. */
@RestController
@Tag(name = "spaces")
@RequiredArgsConstructor
final class SpaceMembersController {

    private final SpaceService spaces;

    private final ApiProperties apiProperties;

    @GetMapping(path = SpacesHttp.MEMBERS_PATH, produces = SpacesHttp.JSON)
    CursorPage<SpaceMemberResponse> listSpaceMembers(
            @PathVariable String slug,
            @RequestParam(required = false) @Positive Integer limit,
            @RequestParam(required = false) String cursor,
            Authentication authentication) {
        var actor = CurrentUser.from(authentication);
        int pageSize = apiProperties.pageSize(limit);
        var after = SpacesHttp.afterMember(cursor);
        var rows = spaces.listMembers(actor, slug, after, pageSize + 1);
        return CursorPage.of(rows, pageSize, SpacesHttp::memberSortKey, SpaceMemberResponse::from);
    }

    @AddSpaceMemberOperation
    ResponseEntity<SpaceMemberResponse> addSpaceMember(
            @PathVariable String slug,
            @Valid @RequestBody AddSpaceMemberRequest request,
            Authentication authentication) {
        var actor = CurrentUser.from(authentication);
        var stored = spaces.addMember(actor, slug, request.toMember());
        return ResponseEntity.status(HttpStatus.CREATED).body(SpaceMemberResponse.from(stored));
    }
}
