package dev.wakey.security;

import dev.wakey.tokens.TokenError;
import dev.wakey.web.ApiException;
import dev.wakey.web.CommonError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/** Logs every 403 with a fixed reason and renders it through the single MVC exception advice. */
@Slf4j
@Component
final class ForbiddenRequestLog implements AccessDeniedHandler {

    private static final int FORBIDDEN_STATUS =
            CommonError.FORBIDDEN.getStatus().value();

    private static final String INSUFFICIENT_SCOPE = "insufficient-scope";

    private static final String INSUFFICIENT_SCOPE_CHALLENGE = "Bearer error=\"insufficient_scope\"";

    private final HandlerExceptionResolver resolver;

    ForbiddenRequestLog(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) {
        if (isInsufficientScope(request)) {
            rejectInsufficientScope(request, response);
        } else {
            RejectReason reason = reasonFor(exception);
            logRejected(reason.getWireName(), request);
            render(request, response, exception);
        }
    }

    /** Rejects a token whose scope is too small, with the RFC 6750 challenge and a fixed reason. */
    void rejectInsufficientScope(HttpServletRequest request, HttpServletResponse response) {
        logRejected(INSUFFICIENT_SCOPE, request);
        if (response.isCommitted()) {
            return;
        }
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, INSUFFICIENT_SCOPE_CHALLENGE);
        render(request, response, new ApiException(TokenError.INSUFFICIENT_SCOPE));
    }

    private void render(HttpServletRequest request, HttpServletResponse response, RuntimeException exception) {
        if (response.isCommitted()) {
            return;
        }
        if (resolver.resolveException(request, response, null, exception) == null) {
            response.setStatus(FORBIDDEN_STATUS);
        }
    }

    private static boolean isInsufficientScope(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof ApiTokenAuthentication token && token.deniesWrite(request.getMethod());
    }

    private static void logRejected(String reason, HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        log.warn("request rejected: reason={} method={} path={}", reason, method, path);
    }

    private static RejectReason reasonFor(AccessDeniedException exception) {
        RejectReason reason = RejectReason.ACCESS_DENIED;
        if (exception instanceof MissingCsrfTokenException) {
            reason = RejectReason.CSRF_TOKEN_MISSING;
        } else if (exception instanceof InvalidCsrfTokenException) {
            reason = RejectReason.CSRF_TOKEN_INVALID;
        }
        return reason;
    }
}
