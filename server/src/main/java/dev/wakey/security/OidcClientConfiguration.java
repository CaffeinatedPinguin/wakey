package dev.wakey.security;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientPropertiesMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.ClientSettings;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/** The {@code oidc} registration with an explicit proof key and the Admin authority mapping. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SecurityProperties.class)
class OidcClientConfiguration {

    private static final String OIDC_REGISTRATION_ID = "oidc";

    private static final String OIDC_USER_NAME_ATTRIBUTE = "sub";

    @Bean
    GrantedAuthoritiesMapper adminAuthoritiesMapper(SecurityProperties properties) {
        return new AdminAuthoritiesMapper(properties.adminClaim(), properties.adminGroup());
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.security.oauth2.client.registration.oidc", name = "client-id")
    @ConditionalOnMissingBean(ClientRegistrationRepository.class)
    ClientRegistrationRepository clientRegistrationRepository(OAuth2ClientProperties properties) {
        Map<String, ClientRegistration> registrations =
                new LinkedHashMap<>(new OAuth2ClientPropertiesMapper(properties).asClientRegistrations());
        ClientRegistration oidc = registrations.get(OIDC_REGISTRATION_ID);
        if (oidc != null) {
            registrations.put(OIDC_REGISTRATION_ID, withProofKey(oidc));
        }
        return new InMemoryClientRegistrationRepository(registrations);
    }

    private static ClientRegistration withProofKey(ClientRegistration registration) {
        ClientSettings settings = ClientSettings.builder().requireProofKey(true).build();
        return ClientRegistration.withClientRegistration(registration)
                .clientSettings(settings)
                .userNameAttributeName(OIDC_USER_NAME_ATTRIBUTE)
                .build();
    }
}
