package dev.wakey.spaces.internal;

import dev.wakey.spaces.SpaceService;
import dev.wakey.spaces.UserSessions;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

@Configuration(proxyBeanMethods = false)
class SpacesConfiguration {

    @Bean
    SpaceService spaceService(JdbcClient jdbc, UserSessions userSessions, Clock clock) {
        return new JdbcSpaceService(jdbc, userSessions, clock);
    }

    @Bean
    UserSessions userSessions(FindByIndexNameSessionRepository<? extends Session> sessions) {
        return new SpringSessionUserSessions(sessions);
    }
}
