package dev.wakey.spaces.web;

import dev.wakey.spaces.SpaceSlug;
import dev.wakey.web.ApiException;
import dev.wakey.web.ApiPaths;
import dev.wakey.web.CommonError;
import dev.wakey.web.CursorCodec;
import java.net.URI;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;

/** The cursor and link helpers of the spaces HTTP surface. */
final class SpacesHttp {

    static final String PATH = ApiPaths.API_V1 + "/spaces";

    static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private SpacesHttp() {}

    static URI location(String slug) {
        return UriComponentsBuilder.fromPath(PATH).pathSegment(slug).build().toUri();
    }

    static String afterSlug(@Nullable String cursor) {
        return cursor == null ? "" : sortValue(cursor);
    }

    private static String sortValue(String encoded) {
        String sortValue = CursorCodec.decode(encoded);
        if (!SpaceSlug.isValid(sortValue)) {
            throw new ApiException(CommonError.INVALID_CURSOR);
        }
        return sortValue;
    }
}
