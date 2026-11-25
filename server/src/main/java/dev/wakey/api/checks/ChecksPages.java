package dev.wakey.api.checks;

import dev.wakey.checks.CheckCursor;
import dev.wakey.checks.CheckService;
import dev.wakey.spaces.SpaceView;
import dev.wakey.web.ApiProperties;
import dev.wakey.web.CursorPage;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Lists and counts the checks of a space. */
@Component
final class ChecksPages {

    private final CheckService checks;

    private final ApiProperties apiProperties;

    private final ChecksAccess access;

    ChecksPages(CheckService checks, ApiProperties apiProperties, ChecksAccess access) {
        this.checks = checks;
        this.apiProperties = apiProperties;
        this.access = access;
    }

    CursorPage<CheckResponse> page(Authentication authentication, String slug, CheckListQuery query) {
        SpaceView space = access.read(authentication, slug);
        int requested = Math.max(1, query.pageSize());
        int pageSize = Math.max(1, apiProperties.capPageSize(requested));
        @Nullable CheckCursor after = ChecksCursor.decode(query.getCursor());
        var filter = query.toFilter();
        var page = ChecksErrors.mapped(() -> checks.list(space.id(), filter, after, pageSize));
        List<CheckResponse> items =
                page.items().stream().map(CheckResponse::from).toList();
        @Nullable String next = ChecksCursor.encode(page.nextCursor());
        return new CursorPage<>(items, next);
    }

    StateCountsResponse counts(Authentication authentication, String slug, CheckFilterQuery query) {
        SpaceView space = access.read(authentication, slug);
        var filter = query.toFilter();
        var counts = ChecksErrors.mapped(() -> checks.counts(space.id(), filter));
        return StateCountsResponse.from(counts);
    }
}
