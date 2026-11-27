package dev.wakey.tokens.web;

import dev.wakey.web.ApiPaths;

/** The paths of the token HTTP surface, shared by the operation annotations. */
final class TokenPaths {

    static final String COLLECTION = ApiPaths.API_V1 + "/spaces/{slug}/tokens";

    static final String ITEM = COLLECTION + "/{id}";

    private TokenPaths() {}
}
