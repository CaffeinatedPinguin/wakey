package dev.wakey.security;

import dev.wakey.web.ApiException;
import dev.wakey.web.CommonError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/** Renders a denied acquire as the shared RFC 9457 response with a whole-second Retry-After. */
@Component
final class RateLimitResponse {

    private static final int TOO_MANY_REQUESTS_STATUS =
            CommonError.TOO_MANY_REQUESTS.getStatus().value();

    private static final long MILLIS_PER_SECOND = 1000L;

    private final HandlerExceptionResolver resolver;

    RateLimitResponse(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    void reject(HttpServletRequest request, HttpServletResponse response, Duration retryAfter) {
        if (response.isCommitted()) {
            return;
        }
        String seconds = Long.toString(wholeSeconds(retryAfter));
        response.setHeader(HttpHeaders.RETRY_AFTER, seconds);
        ApiException exception = new ApiException(CommonError.TOO_MANY_REQUESTS);
        if (resolver.resolveException(request, response, null, exception) == null) {
            response.setStatus(TOO_MANY_REQUESTS_STATUS);
        }
    }

    private static long wholeSeconds(Duration retryAfter) {
        long millis = retryAfter.toMillis();
        long roundedUp = (millis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND;
        return Math.max(1L, roundedUp);
    }
}
