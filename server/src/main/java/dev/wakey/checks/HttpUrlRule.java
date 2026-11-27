package dev.wakey.checks;

import java.net.URI;
import java.net.URISyntaxException;
import lombok.extern.slf4j.Slf4j;

/** The one rule of an HTTP address: a parseable absolute URI with an http(s) scheme and a host. */
@Slf4j
public final class HttpUrlRule {

    private HttpUrlRule() {}

    public static boolean isHttpUrl(String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            boolean httpScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            String host = uri.getHost();
            return httpScheme && host != null && !host.isBlank();
        } catch (URISyntaxException exception) {
            log.debug("Rejected an address that is not a URI", exception);
            return false;
        }
    }
}
