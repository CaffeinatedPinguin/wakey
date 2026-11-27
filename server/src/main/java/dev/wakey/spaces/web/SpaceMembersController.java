package dev.wakey.spaces.web;

import dev.wakey.web.CursorPage;
import dev.wakey.web.PageQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The HTTP adapter for every membership operation of a space. */
@RestController
@RequestMapping(path = SpacesHttp.MEMBERS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
final class SpaceMembersController {

    private final SpaceMembersResources resources;

    @ListSpaceMembersOperation
    CursorPage<SpaceMemberResponse> listSpaceMembers(
            @PathVariable String slug, @Valid PageQuery page, SpaceReader reader) {
        return resources.list(reader, slug, page.limit(), page.cursor());
    }

    @AddSpaceMemberOperation
    ResponseEntity<SpaceMemberResponse> addSpaceMember(
            @PathVariable String slug, SpaceWriter writer, @Valid @RequestBody AddSpaceMemberRequest request) {
        return resources.add(writer, slug, request);
    }

    @ChangeSpaceMemberRoleOperation
    void changeSpaceMemberRole(
            @PathVariable String slug,
            SpaceWriter writer,
            @Valid @ParameterObject SpaceMemberQuery member,
            @Valid @RequestBody ChangeSpaceMemberRoleRequest request) {
        resources.changeRole(writer, slug, member, request);
    }

    @RemoveSpaceMemberOperation
    void removeSpaceMember(
            @PathVariable String slug, SpaceWriter writer, @Valid @ParameterObject SpaceMemberQuery member) {
        resources.remove(writer, slug, member);
    }
}
