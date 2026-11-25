package dev.wakey.spaces.internal;

import dev.wakey.spaces.SpaceError;
import dev.wakey.spaces.UserSessions;
import dev.wakey.web.ErrorCodes;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

/** Wires the remaining spaces adapters; the service itself is a plain component. */
@Configuration(proxyBeanMethods = false)
class SpacesBeansConfiguration {

    @Bean
    UserSessions userSessions(FindByIndexNameSessionRepository<? extends Session> sessions) {
        return new SpringSessionUserSessions(sessions);
    }

    @Bean
    ErrorCodes spaceErrorCodes() {
        return () -> List.of(SpaceError.values());
    }
}
