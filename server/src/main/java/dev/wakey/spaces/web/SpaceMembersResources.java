package dev.wakey.spaces.web;

import dev.wakey.spaces.AddMemberResult;
import dev.wakey.spaces.SpaceMemberId;
import dev.wakey.spaces.SpaceMembers;
import dev.wakey.web.ApiProperties;
import dev.wakey.web.CursorPage;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/** Builds the membership HTTP responses; the controller only maps its arguments to these calls. */
@Component
@RequiredArgsConstructor
final class SpaceMembersResources {

    private final SpaceMembers members;

    private final ApiProperties apiProperties;

    CursorPage<SpaceMemberResponse> list(
            SpaceReader reader, String slug, @Nullable Integer limit, @Nullable String cursor) {
        int pageSize = apiProperties.pageSize(limit);
        var after = SpacesHttp.afterMember(cursor);
        var rows = members.listMembers(reader.user(), slug, after, pageSize + 1);
        return CursorPage.of(rows, pageSize, SpacesHttp::memberSortKey, SpaceMemberResponse::from);
    }

    ResponseEntity<SpaceMemberResponse> add(SpaceWriter writer, String slug, AddSpaceMemberRequest request) {
        AddMemberResult result = members.addMember(writer.user(), slug, request.toMember(), writer.viaToken());
        SpaceMemberResponse response = SpaceMemberResponse.from(result.member());
        if (result.created()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        return ResponseEntity.ok(response);
    }

    void changeRole(SpaceWriter writer, String slug, SpaceMemberQuery member, ChangeSpaceMemberRoleRequest request) {
        SpaceMemberId memberId = member.toId();
        members.changeRole(writer.user(), slug, request.toMember(memberId), writer.viaToken());
    }

    void remove(SpaceWriter writer, String slug, SpaceMemberQuery member) {
        members.removeMember(writer.user(), slug, member.toId(), writer.viaToken());
    }
}
