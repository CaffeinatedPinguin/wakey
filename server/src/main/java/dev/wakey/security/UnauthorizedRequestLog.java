package dev.wakey.security;

import dev.wakey.audit.ActorKind;
import dev.wakey.audit.AuditActor;
import dev.wakey.audit.AuditEntry;
import dev.wakey.audit.AuditLog;
import dev.wakey.audit.AuditOutcome;
import dev.wakey.audit.AuditSource;
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

/** Logs and audits every 401 with a fixed reason and renders it through the single MVC exception advice. */
@Slf4j
@Component
final class UnauthorizedRequestLog implements AuthenticationEntryPoint {

    private static final int UNAUTHENTICATED_STATUS =
            CommonError.UNAUTHENTICATED.getStatus().value();

    private static final String INVALID_TOKEN_CHALLENGE = "Bearer error=\"invalid_token\"";

    private static final String REQUEST_OBJECT_TYPE = "request";

    private final HandlerExceptionResolver resolver;

    private final AuditLog auditLog;

    UnauthorizedRequestLog(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver, AuditLog auditLog) {
        this.resolver = resolver;
        this.auditLog = auditLog;
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
        logRejected(RejectReason.INVALID_TOKEN.getWireName(), request);
        if (response.isCommitted()) {
            return;
        }
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, INVALID_TOKEN_CHALLENGE);
        ApiException exception = new ApiException(TokenError.INVALID_TOKEN);
        if (resolver.resolveException(request, response, null, exception) == null) {
            response.setStatus(UNAUTHENTICATED_STATUS);
        }
    }

    private void logRejected(String reason, HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        log.info("request rejected: reason={} method={} path={}", reason, method, path);
        AuditEntry entry = new AuditEntry(
                "access.denied",
                REQUEST_OBJECT_TYPE,
                reason,
                AuditOutcome.DENIED,
                AuditSource.API,
                new AuditActor(null, ActorKind.SYSTEM),
                null,
                "{}");
        auditLog.recordSecurityEvent(entry);
    }
}
