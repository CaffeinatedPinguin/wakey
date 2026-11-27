package dev.wakey.spaces.web;

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

    private final SpaceMembersResources resources;

    @RequestMapping(method = RequestMethod.GET)
    CursorPage<SpaceMemberResponse> listSpaceMembers(
            @PathVariable String slug,
            @RequestParam(required = false) @Positive Integer limit,
            @RequestParam(required = false) String cursor,
            SpaceReader reader) {
        return resources.list(reader, slug, limit, cursor);
    }

    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<SpaceMemberResponse> addSpaceMember(
            @PathVariable String slug, SpaceWriter writer, @Valid @RequestBody AddSpaceMemberRequest request) {
        return resources.add(writer, slug, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(path = "/role", method = RequestMethod.PUT)
    void changeSpaceMemberRole(
            @PathVariable String slug,
            SpaceWriter writer,
            @Valid @ParameterObject SpaceMemberQuery member,
            @Valid @RequestBody ChangeSpaceMemberRoleRequest request) {
        resources.changeRole(writer, slug, member, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(method = RequestMethod.DELETE)
    void removeSpaceMember(
            @PathVariable String slug, SpaceWriter writer, @Valid @ParameterObject SpaceMemberQuery member) {
        resources.remove(writer, slug, member);
    }
}
