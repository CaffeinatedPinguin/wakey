package dev.wakey.spaces.web;

import dev.wakey.security.CurrentUser;
import dev.wakey.spaces.SpaceMemberId;
import dev.wakey.spaces.SpaceService;
import dev.wakey.web.ApiProperties;
import dev.wakey.web.CursorPage;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The HTTP adapter for every membership operation of a space. */
@RestController
@RequestMapping(path = SpacesHttp.MEMBERS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "spaces")
@RequiredArgsConstructor
final class SpaceMembersController {

    private final SpaceService spaces;

    private final ApiProperties apiProperties;

    @RequestMapping(method = RequestMethod.GET)
    CursorPage<SpaceMemberResponse> listSpaceMembers(
            @PathVariable String slug,
            @RequestParam(required = false) @Positive Integer limit,
            @RequestParam(required = false) String cursor,
            CurrentUser actor) {
        int pageSize = apiProperties.pageSize(limit);
        var after = SpacesHttp.afterMember(cursor);
        var rows = spaces.listMembers(actor, slug, after, pageSize + 1);
        return CursorPage.of(rows, pageSize, SpacesHttp::memberSortKey, SpaceMemberResponse::from);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(method = RequestMethod.POST)
    SpaceMemberResponse addSpaceMember(
            @PathVariable String slug, @Valid @RequestBody AddSpaceMemberRequest request, CurrentUser actor) {
        var stored = spaces.addMember(actor, slug, request.toMember());
        return SpaceMemberResponse.from(stored);
    }

    @RequestMapping(path = "/role", method = RequestMethod.PUT)
    ResponseEntity<Void> changeSpaceMemberRole(
            @PathVariable String slug,
            @Valid @ParameterObject SpaceMemberId member,
            @Valid @RequestBody ChangeSpaceMemberRoleRequest request,
            CurrentUser actor) {
        spaces.changeRole(actor, slug, request.toMember(member));
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(method = RequestMethod.DELETE)
    ResponseEntity<Void> removeSpaceMember(
            @PathVariable String slug, @Valid @ParameterObject SpaceMemberId member, CurrentUser actor) {
        spaces.removeMember(actor, slug, member);
        return ResponseEntity.noContent().build();
    }
}
