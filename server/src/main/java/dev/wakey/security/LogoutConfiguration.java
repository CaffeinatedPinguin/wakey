package dev.wakey.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.web.authentication.logout.HeaderWriterLogoutHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter.Directive;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/** The single logout endpoint shared by the user chain and its request matcher. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class LogoutConfiguration {

    private static final String LOGOUT_PATH = "/logout";

    static String logoutPath() {
        return LOGOUT_PATH;
    }

    static void configure(LogoutConfigurer<HttpSecurity> logout) {
        logout.logoutRequestMatcher(PathPatternRequestMatcher.pathPattern(HttpMethod.POST, LOGOUT_PATH))
                .addLogoutHandler(new HeaderWriterLogoutHandler(new ClearSiteDataHeaderWriter(Directive.COOKIES)))
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler());
    }
}
