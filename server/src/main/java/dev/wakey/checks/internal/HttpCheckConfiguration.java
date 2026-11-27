package dev.wakey.checks.internal;

import dev.wakey.checks.CheckExecutor;
import dev.wakey.checks.EgressPolicy;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.time.Clock;
import javax.net.ssl.SSLException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/** Wires the HTTP executor to the production DNS lookup, clock, address policy and JSON mapper. */
@Configuration(proxyBeanMethods = false)
class HttpCheckConfiguration {

    private static final ApplicationProtocolConfig ALPN = new ApplicationProtocolConfig(
            ApplicationProtocolConfig.Protocol.ALPN,
            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
            ApplicationProtocolNames.HTTP_2,
            ApplicationProtocolNames.HTTP_1_1);

    @Bean(destroyMethod = "close")
    HttpDnsLookup httpDnsLookup() {
        return new HttpDnsLookup();
    }

    @Bean
    HttpCheckEnvironment httpCheckEnvironment(EgressPolicy egressPolicy, HttpDnsLookup dnsLookup) throws SSLException {
        SslContext sslContext = SslContextBuilder.forClient()
                .applicationProtocolConfig(ALPN)
                .endpointIdentificationAlgorithm("HTTPS")
                .build();
        return new HttpCheckEnvironment(egressPolicy::allows, dnsLookup::resolve, sslContext);
    }

    @Bean
    CheckExecutor httpCheckExecutor(HttpCheckEnvironment environment, Clock clock, ObjectMapper objectMapper) {
        HttpCheckOutcome outcome = new HttpCheckOutcome(objectMapper);
        return new HttpCheckExecutor(environment, clock, outcome);
    }
}
