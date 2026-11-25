package dev.wakey.api.spaces;

import dev.wakey.spaces.SpaceSlug;
import dev.wakey.web.ApiException;
import dev.wakey.web.ApiPaths;
import dev.wakey.web.CommonError;
import dev.wakey.web.CursorCodec;
import java.net.URI;
import java.util.Optional;
import org.springframework.web.util.UriComponentsBuilder;

/** The cursor and link helpers of the spaces HTTP surface. */
final class SpacesHttp {

    static final String PATH = ApiPaths.API_V1 + "/spaces";

    private SpacesHttp() {}

    static URI location(String slug) {
        return UriComponentsBuilder.fromPath(PATH).pathSegment(slug).build().toUri();
    }

    static String afterSlug(Optional<String> cursor) {
        return cursor.map(SpacesHttp::sortValue).orElse("");
    }

    static String nextCursor(String slug) {
        return CursorCodec.encode(slug);
    }

    private static String sortValue(String encoded) {
        String sortValue = CursorCodec.decode(encoded);
        if (!SpaceSlug.isValid(sortValue)) {
            throw new ApiException(CommonError.INVALID_CURSOR);
        }
        return sortValue;
    }
}
