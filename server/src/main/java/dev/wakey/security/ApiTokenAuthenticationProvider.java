package dev.wakey.security;

import dev.wakey.tokens.ApiTokens;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/** Turns a presented API token secret into the token's authentication through the tokens module. */
@RequiredArgsConstructor
final class ApiTokenAuthenticationProvider implements AuthenticationProvider {

    private final ApiTokens tokens;

    @Override
    public Authentication authenticate(Authentication authentication) {
        ApiTokenRequest request = (ApiTokenRequest) authentication;
        Optional<ApiTokenAuthentication> result =
                tokens.authenticate(request.secret()).map(ApiTokenAuthentication::new);
        return result.orElseThrow(ApiTokenAuthenticationProvider::invalidToken);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiTokenRequest.class.isAssignableFrom(authentication);
    }

    private static BadCredentialsException invalidToken() {
        return new BadCredentialsException("The API token is invalid");
    }
}
