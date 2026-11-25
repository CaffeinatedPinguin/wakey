package dev.wakey.security;

import dev.wakey.tokens.TokenIdentity;
import dev.wakey.tokens.TokenScope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Enforces the scope of an API token: a read token may not write. The one-space and token
 * management rules live in the HTTP adapters, where the decoded path and the authentication are
 * already available.
 */
final class ApiTokenScopeFilter extends OncePerRequestFilter {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof ApiTokenAuthentication tokenAuthentication) {
            enforceScope(tokenAuthentication.identity(), request, response, filterChain);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private static void enforceScope(
            TokenIdentity identity, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        boolean readTokenWrites = identity.scope() == TokenScope.READ && isWriteRequest(request);
        if (readTokenWrites) {
            ForbiddenRequestLog.rejectInsufficientScope(request, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private static boolean isWriteRequest(HttpServletRequest request) {
        return WRITE_METHODS.contains(request.getMethod());
    }
}
