package dev.wakey.web;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** The HTTP surface prefixes and security endpoints; each module owns the paths below them. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiPaths {

    private static final String ALL = "/**";

    public static final String API_V1 = "/api/v1";

    public static final String API_V1_ALL = API_V1 + ALL;

    public static final String AGENT_V1 = "/agent/v1";

    public static final String AGENT_V1_ALL = AGENT_V1 + ALL;

    public static final String PING = "/ping";

    public static final String PING_ALL = PING + ALL;

    public static final String PUBLIC_V1 = "/public/v1";

    public static final String PUBLIC_V1_ALL = PUBLIC_V1 + ALL;

    public static final String OAUTH2 = "/oauth2";

    public static final String OAUTH2_ALL = OAUTH2 + ALL;

    public static final String LOGIN = "/login";

    public static final String LOGIN_ALL = LOGIN + ALL;

    public static final String OIDC_AUTHORIZATION = OAUTH2 + "/authorization";

    public static final String OIDC_LOGIN_PAGE = OIDC_AUTHORIZATION + "/oidc";

    public static final String LOGOUT = "/logout";

    public static final String HEALTHZ = "/healthz";
}
