package dev.wakey.spaces.web;

import dev.wakey.security.CurrentUser;
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

    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<SpaceMemberResponse> addSpaceMember(
            @PathVariable String slug, @Valid @RequestBody AddSpaceMemberRequest request, CurrentUser actor) {
        var result = spaces.addMember(actor, slug, request.toMember());
        var response = SpaceMemberResponse.from(result.member());
        if (result.created()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(path = "/role", method = RequestMethod.PUT)
    void changeSpaceMemberRole(
            @PathVariable String slug,
            @Valid @ParameterObject SpaceMemberQuery member,
            @Valid @RequestBody ChangeSpaceMemberRoleRequest request,
            CurrentUser actor) {
        var memberId = member.toId();
        var updated = request.toMember(memberId);
        spaces.changeRole(actor, slug, updated);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(method = RequestMethod.DELETE)
    void removeSpaceMember(
            @PathVariable String slug, @Valid @ParameterObject SpaceMemberQuery member, CurrentUser actor) {
        spaces.removeMember(actor, slug, member.toId());
    }
}
