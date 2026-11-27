package dev.wakey.security;

import dev.wakey.tokens.ApiTokens;
import dev.wakey.web.ApiPaths;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CsrfFilter;

/** The three filter chains and the request logging shared by every surface. */
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
class SecurityConfiguration {

    private final UnauthorizedRequestLog unauthorized;

    private final ForbiddenRequestLog forbidden;

    private final ApiTokens apiTokens;

    private final RateLimitFilters rateLimitFilters;

    @Bean
    @Order(1)
    SecurityFilterChain statelessSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(ApiPaths.AGENT_V1_ALL, ApiPaths.PING_ALL, ApiPaths.PUBLIC_V1_ALL)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // This chain is STATELESS and never authenticates with a cookie, so CSRF has nothing to
                // protect; statelessSurfaceIgnoresTheSessionCookie proves the session cookie is ignored.
                .csrf(csrf -> csrf.disable())
                .requestCache(cache -> cache.disable())
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden))
                .headers(SecurityHeaders::applyTo);
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
        http.securityMatcher(
                        ApiPaths.API_V1_ALL, ApiPaths.OAUTH2_ALL, ApiPaths.LOGIN_ALL, LogoutConfiguration.logoutPath())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(ApiPaths.OAUTH2_ALL, ApiPaths.LOGIN_ALL)
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, ApiPaths.API_V1_ALL)
                        .authenticated()
                        .requestMatchers(ApiPaths.API_V1_ALL)
                        .access(ApiTokenSecurity.writeAccess())
                        .anyRequest()
                        .authenticated())
                .csrf(csrf -> csrf.spa().ignoringRequestMatchers(ApiTokenSecurity.bearerApiRequests()))
                .requestCache(cache -> cache.disable())
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(unauthorized, ApiTokenSecurity.apiRequests())
                        .accessDeniedHandler(forbidden))
                .logout(LogoutConfiguration::configure)
                .headers(SecurityHeaders::applyTo);
        SessionMaxAgeFilter maxAgeFilter = new SessionMaxAgeFilter(clock, properties.sessionMaxAge());
        http.addFilterBefore(maxAgeFilter, SecurityContextHolderFilter.class);
        http.addFilterBefore(rateLimitFilters.loginFilter(), SecurityContextHolderFilter.class);
        var tokenFilter = ApiTokenSecurity.authenticationFilter(apiTokens, unauthorized);
        http.addFilterBefore(tokenFilter, CsrfFilter.class);
        http.addFilterAfter(rateLimitFilters.apiTokenFilter(), CsrfFilter.class);
        if (registrations.getIfAvailable() != null) {
            configureLogin(http, properties.frontendUrl());
        }
        return http.build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(ApiPaths.HEALTHZ)
                        .permitAll()
                        .anyRequest()
                        .denyAll())
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden))
                .headers(SecurityHeaders::applyTo);
        return http.build();
    }

    private static void configureLogin(HttpSecurity http, String frontendUrl) throws Exception {
        http.oauth2Login(oauth2 -> oauth2.loginPage(ApiPaths.OIDC_LOGIN_PAGE)
                .defaultSuccessUrl(frontendUrl, true)
                .failureHandler(new LoginFailureRedirect(frontendUrl)));
    }
}
