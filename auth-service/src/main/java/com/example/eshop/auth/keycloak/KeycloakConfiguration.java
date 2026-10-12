package com.example.eshop.auth.keycloak;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.boot.web.client.RestTemplateBuilder;
import java.time.Duration;

@Configuration
public class KeycloakConfiguration {
    public static OAuth2TokenValidator<Jwt> validator(String issuer, String audience, String clientId) {
        return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), jwt -> {
            boolean valid = jwt.getAudience().contains(audience)
                    && clientId.equals(jwt.getClaimAsString("azp"))
                    && jwt.getSubject() != null && !jwt.getSubject().isBlank()
                    && jwt.getExpiresAt() != null;
            return valid ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Invalid audience, client or subject", null));
        });
    }

    @Bean("keycloakJwtDecoder")
    JwtDecoder decoder(
            @Value("${eshop.keycloak.issuer:http://localhost:8180/realms/eshop}") String issuer,
            @Value("${eshop.keycloak.jwk-set-uri:http://localhost:8180/realms/eshop/protocol/openid-connect/certs}") String jwks,
            @Value("${eshop.keycloak.audience:eshop-api}") String audience,
            @Value("${eshop.keycloak.client-id:eshop-web}") String clientId) {
        // RS256 only; no network call at startup, bounded provider lookup, cached keys.
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwks)
                .restOperations(new RestTemplateBuilder().connectTimeout(Duration.ofSeconds(3))
                        .readTimeout(Duration.ofSeconds(5)).build()).build();
        decoder.setJwtValidator(validator(issuer, audience, clientId));
        return decoder;
    }
}
