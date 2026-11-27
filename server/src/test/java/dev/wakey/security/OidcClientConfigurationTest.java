package dev.wakey.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

class OidcClientConfigurationTest {

    @Test
    void theOidcUserNameIsTheSubjectWhateverTheConfiguration() {
        // given
        OAuth2ClientProperties properties = propertiesWithUserNameAttribute("preferred_username");

        // when
        ClientRegistrationRepository repository =
                new OidcClientConfiguration().clientRegistrationRepository(properties);
        ClientRegistration oidc = Objects.requireNonNull(repository.findByRegistrationId("oidc"));

        // then
        String userNameAttribute =
                oidc.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        assertThat(userNameAttribute).isEqualTo("sub");
    }

    private static OAuth2ClientProperties propertiesWithUserNameAttribute(String userNameAttribute) {
        OAuth2ClientProperties.Provider provider = new OAuth2ClientProperties.Provider();
        provider.setAuthorizationUri("https://issuer.example.com/authorize");
        provider.setTokenUri("https://issuer.example.com/token");
        provider.setUserInfoUri("https://issuer.example.com/userinfo");
        provider.setUserNameAttribute(userNameAttribute);
        OAuth2ClientProperties.Registration registration = new OAuth2ClientProperties.Registration();
        registration.setProvider("oidc");
        registration.setClientId("client-id");
        registration.setClientSecret("client-secret");
        registration.setAuthorizationGrantType("authorization_code");
        registration.setRedirectUri("https://app.example.com/login/oauth2/code/oidc");
        OAuth2ClientProperties properties = new OAuth2ClientProperties();
        properties.getProvider().put("oidc", provider);
        properties.getRegistration().put("oidc", registration);
        return properties;
    }
}
