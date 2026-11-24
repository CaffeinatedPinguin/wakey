package dev.wakey.api.spaces;

import dev.wakey.security.CurrentUser;
import dev.wakey.spaces.SpacePrincipal;
import dev.wakey.spaces.SpaceService;
import dev.wakey.spaces.SpaceView;
import dev.wakey.web.ApiProperties;
import dev.wakey.web.CursorCodec;
import dev.wakey.web.CursorPage;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Resolves the caller and turns the spaces module results into HTTP responses and keyset pages. */
@Component
final class SpacesSupport {

    private static final int EXTRA_ROW = 1;

    private final SpaceService spaces;

    private final ApiProperties apiProperties;

    SpacesSupport(SpaceService spaces, ApiProperties apiProperties) {
        this.spaces = spaces;
        this.apiProperties = apiProperties;
    }

    SpacePrincipal principal(Authentication authentication) {
        CurrentUser user = CurrentUser.from(authentication);
        return new SpacePrincipal(user.subject(), user.admin());
    }

    CursorPage<SpaceResponse> page(SpacePrincipal actor, int limit, Optional<String> cursor) {
        int requested = Math.max(1, limit);
        int pageSize = Math.max(1, apiProperties.capPageSize(requested));
        String afterSlug = cursor.map(SpacesSupport::sortValue).orElse("");
        List<SpaceView> views = spaces.listVisible(actor, afterSlug, pageSize + EXTRA_ROW);
        boolean hasMore = views.size() > pageSize;
        List<SpaceView> current = hasMore ? views.subList(0, pageSize) : views;
        List<SpaceResponse> items = current.stream().map(SpaceResponse::from).toList();
        return new CursorPage<>(items, nextCursor(current, hasMore));
    }

    SpaceResponse find(SpacePrincipal actor, String slug) {
        SpaceView view = spaces.view(actor, slug);
        return SpaceResponse.from(view);
    }

    private static String sortValue(String encoded) {
        return CursorCodec.decode(encoded).sortValue();
    }

    private static @Nullable String nextCursor(List<SpaceView> page, boolean hasMore) {
        if (!hasMore) {
            return null;
        }
        SpaceView last = page.get(page.size() - EXTRA_ROW);
        String id = last.id().toString();
        return CursorCodec.encode(last.slug(), id);
    }
}
