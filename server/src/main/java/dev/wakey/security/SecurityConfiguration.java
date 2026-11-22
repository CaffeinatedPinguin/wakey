package dev.wakey.security;

import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/** The three filter chains and the headers shared by every surface. */
@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

    private static final RequestMatcher API_REQUESTS = PathPatternRequestMatcher.pathPattern("/api/v1/**");

    /** The provider login URL; setting it also stops Spring from generating HTML login and logout pages. */
    private static final String LOGIN_PAGE = "/oauth2/authorization/oidc";

    @Bean
    @Order(1)
    SecurityFilterChain statelessSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/agent/v1/**", "/ping/**", "/public/v1/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .requestCache(cache -> cache.disable())
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .headers(SecuritySupport::applyHeaders);
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain userSecurityFilterChain(
            HttpSecurity http,
            ObjectProvider<ClientRegistrationRepository> registrations,
            SecurityProperties properties,
            Clock clock)
            throws Exception {
        http.securityMatcher("/api/v1/**", "/oauth2/**", "/login/**", "/logout")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/oauth2/**", "/login/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .csrf(csrf -> csrf.spa())
                .requestCache(cache -> cache.disable())
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), API_REQUESTS))
                .logout(SecuritySupport::configureLogout)
                .headers(SecuritySupport::applyHeaders);
        SessionMaxAgeFilter maxAgeFilter = new SessionMaxAgeFilter(clock, properties.sessionMaxAge());
        http.addFilterBefore(maxAgeFilter, SecurityContextHolderFilter.class);
        if (registrations.getIfAvailable() != null) {
            configureLogin(http, properties.frontendUrl());
        }
        return http.build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/healthz")
                        .permitAll()
                        .anyRequest()
                        .denyAll())
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .headers(SecuritySupport::applyHeaders);
        return http.build();
    }

    private static void configureLogin(HttpSecurity http, String frontendUrl) throws Exception {
        http.oauth2Login(oauth2 -> oauth2.loginPage(LOGIN_PAGE)
                .defaultSuccessUrl(frontendUrl, true)
                .failureHandler(SecuritySupport.failureHandler(frontendUrl)));
    }
}
