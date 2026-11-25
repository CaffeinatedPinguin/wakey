package dev.wakey.audit.internal;

import dev.wakey.audit.AuditLog;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration(proxyBeanMethods = false)
class AuditConfiguration {

    @Bean
    AuditLog auditLog(JdbcClient jdbc, Clock clock) {
        return new JdbcAuditLog(jdbc, clock);
    }
}
