package dev.wakey.security;

import dev.wakey.web.CommonError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/** Logs every 401 with a fixed reason and renders it through the single MVC exception advice. */
@Slf4j
@Component
final class UnauthorizedRequestLog implements AuthenticationEntryPoint {

    private static final int UNAUTHENTICATED_STATUS =
            CommonError.UNAUTHENTICATED.getStatus().value();

    private final HandlerExceptionResolver resolver;

    UnauthorizedRequestLog(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        log.info(
                "request rejected: reason={} method={} path={}",
                RejectReason.MISSING_AUTHENTICATION.getWireName(),
                method,
                path);
        if (response.isCommitted()) {
            return;
        }
        if (resolver.resolveException(request, response, null, exception) == null) {
            response.setStatus(UNAUTHENTICATED_STATUS);
        }
    }
}
