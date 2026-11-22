package dev.wakey.messaging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration(proxyBeanMethods = false)
class MessagingConfiguration {

    @Bean
    MessageQueue messageQueue(JdbcClient jdbc) {
        return new JdbcMessageQueue(jdbc);
    }
}
