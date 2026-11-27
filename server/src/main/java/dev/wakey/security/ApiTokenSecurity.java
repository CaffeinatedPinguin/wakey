package dev.wakey.security;

import dev.wakey.tokens.ApiTokens;
import dev.wakey.web.ApiPaths;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.AuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/** The API token filter, its request matchers and the write-scope rule of the user chain. */
final class ApiTokenSecurity {

    private static final RequestMatcher API_REQUESTS = PathPatternRequestMatcher.pathPattern(ApiPaths.API_V1_ALL);

    private static final RequestMatcher BEARER_API_REQUESTS =
            request -> API_REQUESTS.matches(request) && BearerApiTokenConverter.hasBearerSecret(request);

    /** Authenticated session users and write tokens may write; a read token may not. */
    private static final AuthorizationManager<RequestAuthorizationContext> API_WRITE_ACCESS =
            (authentication, context) -> {
                @Nullable Authentication current = authentication.get();
                String method = context.getRequest().getMethod();
                return new AuthorizationDecision(allowsWrite(current, method));
            };

    private ApiTokenSecurity() {}

    static RequestMatcher apiRequests() {
        return API_REQUESTS;
    }

    static RequestMatcher bearerApiRequests() {
        return BEARER_API_REQUESTS;
    }

    static AuthorizationManager<RequestAuthorizationContext> writeAccess() {
        return API_WRITE_ACCESS;
    }

    static AuthenticationFilter authenticationFilter(ApiTokens tokens, UnauthorizedRequestLog unauthorized) {
        AuthenticationProvider provider = new ApiTokenAuthenticationProvider(tokens);
        AuthenticationManager manager = new ProviderManager(provider);
        AuthenticationFilter filter = new AuthenticationFilter(manager, new BearerApiTokenConverter());
        filter.setRequestMatcher(API_REQUESTS);
        filter.setFailureHandler(
                (request, response, unusedException) -> unauthorized.rejectInvalidToken(request, response));
        // The four-argument default calls this handler and then continues the chain; the default
        // SavedRequestAware handler would redirect instead.
        filter.setSuccessHandler((unusedRequest, unusedResponse, unusedAuthentication) -> {});
        return filter;
    }

    private static boolean allowsWrite(@Nullable Authentication authentication, String method) {
        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        boolean readOnlyToken = authentication instanceof ApiTokenAuthentication token && token.deniesWrite(method);
        return authenticated && !readOnlyToken;
    }
}
