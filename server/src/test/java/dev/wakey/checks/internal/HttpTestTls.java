package dev.wakey.checks.internal;

import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.jspecify.annotations.Nullable;

/** Loads the test-only PKCS12 key and certificate and builds the matching client trust context. */
final class HttpTestTls {

    private static final String KEY_STORE = "/tls/check-test.p12";

    private static final String ALIAS = "check-test";

    private static final char[] PASSWORD = "test-only-password".toCharArray();

    private static final ApplicationProtocolConfig ALPN = new ApplicationProtocolConfig(
            ApplicationProtocolConfig.Protocol.ALPN,
            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
            ApplicationProtocolNames.HTTP_2,
            ApplicationProtocolNames.HTTP_1_1);

    private HttpTestTls() {}

    static SSLContext serverContext() {
        try {
            KeyStore keyStore = keyStore();
            KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagers.init(keyStore, PASSWORD);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(keyManagers.getKeyManagers(), null, null);
            return context;
        } catch (GeneralSecurityException | IOException exception) {
            throw new IllegalStateException("Cannot build the test TLS server context", exception);
        }
    }

    static SslContext nettyServerContext() {
        try {
            KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagers.init(keyStore(), PASSWORD);
            return SslContextBuilder.forServer(keyManagers)
                    .applicationProtocolConfig(ALPN)
                    .build();
        } catch (GeneralSecurityException | IOException exception) {
            throw new IllegalStateException("Cannot build the test Netty server context", exception);
        }
    }

    static SslContext clientContext() {
        try {
            Certificate certificate = keyStore().getCertificate(ALIAS);
            if (!(certificate instanceof X509Certificate x509)) {
                throw new IllegalStateException("Missing test certificate " + ALIAS);
            }
            return SslContextBuilder.forClient()
                    .trustManager(x509)
                    .applicationProtocolConfig(ALPN)
                    .endpointIdentificationAlgorithm("HTTPS")
                    .build();
        } catch (GeneralSecurityException | IOException exception) {
            throw new IllegalStateException("Cannot build the test TLS client context", exception);
        }
    }

    private static KeyStore keyStore() throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        @Nullable InputStream input = HttpTestTls.class.getResourceAsStream(KEY_STORE);
        if (input == null) {
            throw new IllegalStateException("Missing test keystore " + KEY_STORE);
        }
        try (InputStream stream = input) {
            keyStore.load(stream, PASSWORD);
        }
        return keyStore;
    }
}
