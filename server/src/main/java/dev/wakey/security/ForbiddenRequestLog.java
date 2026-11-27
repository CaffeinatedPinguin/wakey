package dev.wakey.security;

import dev.wakey.audit.ActorKind;
import dev.wakey.audit.AuditActor;
import dev.wakey.audit.AuditEntry;
import dev.wakey.audit.AuditLog;
import dev.wakey.audit.AuditOutcome;
import dev.wakey.audit.AuditSource;
import dev.wakey.web.ApiException;
import dev.wakey.web.CommonError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/** Logs and audits every 403 with a fixed reason and renders it through the single MVC exception advice. */
@Slf4j
@Component
final class ForbiddenRequestLog implements AccessDeniedHandler {

    private static final int FORBIDDEN_STATUS =
            CommonError.FORBIDDEN.getStatus().value();

    private static final String INSUFFICIENT_SCOPE_CHALLENGE = "Bearer error=\"insufficient_scope\"";

    private static final String REQUEST_OBJECT_TYPE = "request";

    private final HandlerExceptionResolver resolver;

    private final AuditLog auditLog;

    ForbiddenRequestLog(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver, AuditLog auditLog) {
        this.resolver = resolver;
        this.auditLog = auditLog;
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
        logRejected(RejectReason.INSUFFICIENT_SCOPE.getWireName(), request);
        if (response.isCommitted()) {
            return;
        }
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, INSUFFICIENT_SCOPE_CHALLENGE);
        render(request, response, new ApiException(CommonError.INSUFFICIENT_SCOPE));
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

    private void logRejected(String reason, HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        log.warn("request rejected: reason={} method={} path={}", reason, method, path);
        AuditEntry entry = new AuditEntry(
                "access.denied",
                REQUEST_OBJECT_TYPE,
                reason,
                AuditOutcome.DENIED,
                AuditSource.API,
                actor(),
                null,
                "{}");
        auditLog.recordSecurityEvent(entry);
    }

    private static AuditActor actor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return new AuditActor(null, ActorKind.SYSTEM);
        }
        boolean token = authentication instanceof ApiTokenAuthentication;
        return AuditActor.of(authentication.getName(), token);
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
