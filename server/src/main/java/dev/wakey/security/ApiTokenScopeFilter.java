package dev.wakey.security;

import dev.wakey.tokens.TokenIdentity;
import dev.wakey.tokens.TokenScope;
import dev.wakey.web.ApiPaths;
import dev.wakey.web.ProblemDetailsWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Enforces the scope and the one space of an API token: a read token may not write, a token never
 * sees another space, and a token cannot manage tokens. The space list rule lives in the spaces
 * controller, which reads the token's space from {@link CurrentUser}.
 */
final class ApiTokenScopeFilter extends OncePerRequestFilter {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private static final String SPACES_PATH = ApiPaths.API_V1 + "/spaces";

    private static final String SPACES_PREFIX = SPACES_PATH + "/";

    private static final String TOKENS_SEGMENT = "tokens";

    private static final String INSUFFICIENT_SCOPE_DETAIL = "The API token does not allow this request.";

    private static final String NOT_FOUND_DETAIL = "The requested resource was not found.";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof ApiTokenAuthentication tokenAuthentication) {
            applyTokenScope(tokenAuthentication.identity(), request, response, filterChain);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private static void applyTokenScope(
            TokenIdentity identity, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = requestPath(request);
        if (isTokenManagement(path)) {
            insufficientScope(response);
        } else if (isForeignSpace(path, identity.spaceSlug())) {
            notFound(response);
        } else if (isWriteRequest(request) && identity.scope() == TokenScope.READ) {
            insufficientScope(response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private static boolean isTokenManagement(String path) {
        boolean management = false;
        if (path.startsWith(SPACES_PREFIX)) {
            String[] segments = restSegments(path);
            management = segments.length >= 2 && TOKENS_SEGMENT.equals(segments[1]);
        }
        return management;
    }

    private static boolean isForeignSpace(String path, String spaceSlug) {
        boolean foreign = false;
        if (path.startsWith(SPACES_PREFIX)) {
            String requestedSlug = requestedSlug(path);
            foreign = !requestedSlug.equals(spaceSlug);
        }
        return foreign;
    }

    private static boolean isWriteRequest(HttpServletRequest request) {
        return WRITE_METHODS.contains(request.getMethod());
    }

    private static String requestedSlug(String path) {
        String remainder = path.substring(SPACES_PREFIX.length());
        int endOfSlug = remainder.indexOf('/');
        return endOfSlug < 0 ? remainder : remainder.substring(0, endOfSlug);
    }

    private static String[] restSegments(String path) {
        String remainder = path.substring(SPACES_PREFIX.length());
        return remainder.split("/");
    }

    private static String requestPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        return uri.substring(contextPath.length());
    }

    private static void insufficientScope(HttpServletResponse response) throws IOException {
        ProblemDetailsWriter.write(response, HttpStatus.FORBIDDEN, "insufficient_scope", INSUFFICIENT_SCOPE_DETAIL);
    }

    private static void notFound(HttpServletResponse response) throws IOException {
        ProblemDetailsWriter.write(response, HttpStatus.NOT_FOUND, "not_found", NOT_FOUND_DETAIL);
    }
}
