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

    private static final int FORBIDDEN_STATUS =
            CommonError.FORBIDDEN.getStatus().value();

    private final HandlerExceptionResolver resolver;

    ForbiddenRequestLog(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) {
        RejectReason reason = reasonFor(exception);
        String method = request.getMethod();
        String path = request.getRequestURI();
        log.warn("request rejected: reason={} method={} path={}", reason.getWireName(), method, path);
        if (response.isCommitted()) {
            return;
        }
        if (resolver.resolveException(request, response, null, exception) == null) {
            response.setStatus(FORBIDDEN_STATUS);
        }
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
