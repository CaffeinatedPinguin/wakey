package dev.wakey.spaces.web;

import dev.wakey.spaces.SpaceService;
import dev.wakey.web.ApiProperties;
import dev.wakey.web.CurrentUser;
import dev.wakey.web.CursorPage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/** Builds the space HTTP responses; the controller only maps its arguments to these calls. */
@Component
@RequiredArgsConstructor
final class SpacesResources {

    private final SpaceService spaces;

    private final ApiProperties apiProperties;

    CursorPage<SpaceResponse> list(SpaceCaller caller, @Nullable Integer limit, @Nullable String cursor) {
        CurrentUser actor = caller.user();
        String tokenSpaceSlug = caller.slug();
        if (!tokenSpaceSlug.isEmpty()) {
            var view = spaces.view(actor, tokenSpaceSlug);
            SpaceResponse response = SpaceResponse.from(view);
            return new CursorPage<>(List.of(response), null);
        }
        int pageSize = apiProperties.pageSize(limit);
        String afterSlug = SpacesHttp.afterSlug(cursor);
        var rows = spaces.listVisible(actor, afterSlug, pageSize + 1);
        return CursorPage.of(rows, pageSize, view -> view.slug(), SpaceResponse::from);
    }

    SpaceResponse get(SpaceReader reader) {
        return SpaceResponse.from(reader.space());
    }

    ResponseEntity<SpaceResponse> create(SpaceCaller caller, CreateSpaceRequest request) {
        CurrentUser actor = caller.user();
        var result = spaces.create(actor, request.slug(), request.name());
        String storedSlug = result.space().slug();
        var view = spaces.view(actor, storedSlug);
        return ResponseEntity.created(SpacesHttp.location(storedSlug)).body(SpaceResponse.from(view));
    }

    ResponseEntity<SpaceResponse> put(String slug, SpaceUpsert upsert, PutSpaceRequest request) {
        CurrentUser actor = upsert.user();
        var result = spaces.put(actor, slug, request.name(), request.version());
        String storedSlug = result.space().slug();
        var view = spaces.view(actor, storedSlug);
        if (result.created()) {
            return ResponseEntity.created(SpacesHttp.location(storedSlug)).body(SpaceResponse.from(view));
        }
        return ResponseEntity.ok(SpaceResponse.from(view));
    }
}
