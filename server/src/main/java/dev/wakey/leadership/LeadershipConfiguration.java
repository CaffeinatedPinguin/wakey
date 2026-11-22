package dev.wakey.leadership;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration(proxyBeanMethods = false)
class LeadershipConfiguration {

    @Bean
    LeaderLease leaderLease(
            JdbcClient jdbc,
            @Value("${wakey.leadership.holder-id}") String holderId,
            @Value("${wakey.leadership.ttl}") Duration ttl) {
        return new JdbcLeaderLease(jdbc, holderId, ttl);
    }
}
