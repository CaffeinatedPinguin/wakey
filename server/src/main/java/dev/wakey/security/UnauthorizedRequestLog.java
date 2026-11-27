package dev.wakey.security;

import dev.wakey.tokens.TokenError;
import dev.wakey.web.ApiException;
import dev.wakey.web.CommonError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
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

    private static final String INVALID_TOKEN = "invalid-token";

    private static final String INVALID_TOKEN_CHALLENGE = "Bearer error=\"invalid_token\"";

    private final HandlerExceptionResolver resolver;

    UnauthorizedRequestLog(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        logRejected(RejectReason.MISSING_AUTHENTICATION.getWireName(), request);
        if (response.isCommitted()) {
            return;
        }
        if (resolver.resolveException(request, response, null, exception) == null) {
            response.setStatus(UNAUTHENTICATED_STATUS);
        }
    }

    /** Rejects a presented token with the RFC 6750 challenge and a fixed reason. */
    void rejectInvalidToken(HttpServletRequest request, HttpServletResponse response) {
        logRejected(INVALID_TOKEN, request);
        if (response.isCommitted()) {
            return;
        }
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, INVALID_TOKEN_CHALLENGE);
        ApiException exception = new ApiException(TokenError.INVALID_TOKEN);
        if (resolver.resolveException(request, response, null, exception) == null) {
            response.setStatus(UNAUTHENTICATED_STATUS);
        }
    }

    private static void logRejected(String reason, HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        log.info("request rejected: reason={} method={} path={}", reason, method, path);
    }
}
