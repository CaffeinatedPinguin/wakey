package dev.wakey.spaces.web;

import dev.wakey.security.CurrentUser;
import dev.wakey.spaces.SpaceMemberId;
import dev.wakey.spaces.SpaceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** The HTTP adapter for changing and removing the memberships of a space. */
@RestController
@Tag(name = "spaces")
@RequiredArgsConstructor
final class SpaceMemberRolesController {

    private final SpaceService spaces;

    @ChangeSpaceMemberRoleOperation
    ResponseEntity<Void> changeSpaceMemberRole(
            @PathVariable String slug,
            @Valid @ParameterObject SpaceMemberId member,
            @Valid @RequestBody ChangeSpaceMemberRoleRequest request,
            Authentication authentication) {
        var actor = CurrentUser.from(authentication);
        spaces.changeRole(actor, slug, request.toMember(member));
        return ResponseEntity.noContent().build();
    }

    @RemoveSpaceMemberOperation
    ResponseEntity<Void> removeSpaceMember(
            @PathVariable String slug, @Valid @ParameterObject SpaceMemberId member, Authentication authentication) {
        var actor = CurrentUser.from(authentication);
        spaces.removeMember(actor, slug, member);
        return ResponseEntity.noContent().build();
    }
}
