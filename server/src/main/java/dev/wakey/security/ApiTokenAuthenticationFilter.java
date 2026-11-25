package dev.wakey.security;

import dev.wakey.tokens.ApiTokens;
import dev.wakey.tokens.TokenIdentity;
import dev.wakey.web.ApiPaths;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates an {@code Authorization: Bearer wk_...} request on {@code /api/v1/**}. A bad token is
 * rejected even when the request also carries a valid session; CSRF is skipped elsewhere.
 */
final class ApiTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final ApiTokens tokens;

    ApiTokenAuthenticationFilter(ApiTokens tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        @Nullable String secret = bearerSecret(request);
        if (secret == null) {
            filterChain.doFilter(request, response);
        } else {
            authenticate(secret, request, response, filterChain);
        }
    }

    /** True when the current request was authenticated with an API token; the CSRF filter uses it. */
    static boolean isTokenAuthenticated(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean apiRequest = isApiRequest(request);
        return apiRequest && authentication instanceof ApiTokenAuthentication;
    }

    private void authenticate(
            String secret, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Optional<TokenIdentity> identity = tokens.authenticate(secret);
        if (identity.isEmpty()) {
            UnauthorizedRequestLog.rejectInvalidToken(request, response);
        } else {
            setTokenAuthentication(identity.get());
            filterChain.doFilter(request, response);
        }
    }

    private static void setTokenAuthentication(TokenIdentity identity) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new ApiTokenAuthentication(identity));
        SecurityContextHolder.setContext(context);
    }

    private static @Nullable String bearerSecret(HttpServletRequest request) {
        if (!isApiRequest(request)) {
            return null;
        }
        @Nullable String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        return secretFromHeader(header);
    }

    private static @Nullable String secretFromHeader(@Nullable String header) {
        @Nullable String secret = null;
        if (header != null && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            secret = header.substring(BEARER_PREFIX.length()).trim();
        }
        return secret;
    }

    private static boolean isApiRequest(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        String path = uri.substring(contextPath.length());
        return path.equals(ApiPaths.API_V1) || path.startsWith(ApiPaths.API_V1 + "/");
    }
}
