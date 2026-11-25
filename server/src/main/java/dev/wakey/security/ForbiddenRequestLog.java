package dev.wakey.security;

import dev.wakey.web.CommonError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/** Logs every 403 with a fixed reason and renders it through the single MVC exception advice. */
@Slf4j
@Component
final class ForbiddenRequestLog implements AccessDeniedHandler {

    private static final String ACCESS_DENIED = "access-denied";

    private static final int FORBIDDEN_STATUS =
            CommonError.FORBIDDEN.getStatus().value();

    private static final String CSRF_TOKEN_MISSING = "csrf-token-missing";

    private static final String CSRF_TOKEN_INVALID = "csrf-token-invalid";

    private final HandlerExceptionResolver resolver;

    ForbiddenRequestLog(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) {
        String reason = reasonFor(exception);
        String method = request.getMethod();
        String path = request.getRequestURI();
        log.warn("request rejected: reason={} method={} path={}", reason, method, path);
        if (response.isCommitted()) {
            return;
        }
        if (resolver.resolveException(request, response, null, exception) == null) {
            response.setStatus(FORBIDDEN_STATUS);
        }
    }

    private static String reasonFor(AccessDeniedException exception) {
        String reason = ACCESS_DENIED;
        if (exception instanceof MissingCsrfTokenException) {
            reason = CSRF_TOKEN_MISSING;
        } else if (exception instanceof InvalidCsrfTokenException) {
            reason = CSRF_TOKEN_INVALID;
        }
        return reason;
    }
}
