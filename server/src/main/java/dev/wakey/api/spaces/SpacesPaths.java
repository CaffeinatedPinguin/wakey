package dev.wakey.api.spaces;

import dev.wakey.web.ApiPaths;

/** The paths of the spaces HTTP surface, shared by the list and item controllers. */
final class SpacesPaths {

    static final String COLLECTION = ApiPaths.API_V1 + "/spaces";

    static final String ITEM = COLLECTION + "/{slug}";

    private SpacesPaths() {}
}
