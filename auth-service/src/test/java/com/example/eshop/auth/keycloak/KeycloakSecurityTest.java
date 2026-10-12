package com.example.eshop.auth.keycloak;

import com.example.eshop.auth.service.impl.AuthServiceImpl;
import com.example.eshop.common.audit.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class KeycloakSecurityTest {
    @Test void realDecoderVerifiesSignatureUsingProviderKeys() throws Exception {
        var key = new com.nimbusds.jose.jwk.gen.RSAKeyGenerator(2048).keyID("test-key").generate();
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/certs", exchange -> {
            byte[] body = new com.nimbusds.jose.jwk.JWKSet(key.toPublicJWK()).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            var claims = new com.nimbusds.jwt.JWTClaimsSet.Builder().issuer(ISSUER).subject("linked-user")
                    .audience("eshop-api").claim("azp", "eshop-web")
                    .expirationTime(java.util.Date.from(Instant.now().plusSeconds(300))).build();
            var jwt = new com.nimbusds.jwt.SignedJWT(new com.nimbusds.jose.JWSHeader.Builder(com.nimbusds.jose.JWSAlgorithm.RS256).keyID("test-key").build(), claims);
            jwt.sign(new com.nimbusds.jose.crypto.RSASSASigner(key));
            var decoder = new KeycloakConfiguration().decoder(ISSUER,
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/certs", "eshop-api", "eshop-web");
            assertThat(decoder.decode(jwt.serialize()).getSubject()).isEqualTo("linked-user");
            var wrongKey = new com.nimbusds.jose.jwk.gen.RSAKeyGenerator(2048).generate();
            var forged = new com.nimbusds.jwt.SignedJWT(jwt.getHeader(), claims);
            forged.sign(new com.nimbusds.jose.crypto.RSASSASigner(wrongKey));
            assertThatThrownBy(() -> decoder.decode(forged.serialize())).isInstanceOf(JwtException.class);
        } finally { server.stop(0); }
    }
    private static final String ISSUER = "http://localhost:8180/realms/eshop";
    Jwt token(String issuer, String audience, String client, Instant expires) {
        return Jwt.withTokenValue("test").header("alg", "RS256").issuer(issuer).subject("subject-id")
                .audience(List.of(audience)).claim("azp", client)
                .issuedAt(Instant.now().minusSeconds(600)).expiresAt(expires).build();
    }
    @Test void validatesIssuerAudienceClientAndExpiry() {
        var validator = KeycloakConfiguration.validator(ISSUER, "eshop-api", "eshop-web");
        Instant future = Instant.now().plusSeconds(300);
        assertThat(validator.validate(token(ISSUER, "eshop-api", "eshop-web", future)).hasErrors()).isFalse();
        assertThat(validator.validate(token("https://evil.test", "eshop-api", "eshop-web", future)).hasErrors()).isTrue();
        assertThat(validator.validate(token(ISSUER, "other-api", "eshop-web", future)).hasErrors()).isTrue();
        assertThat(validator.validate(token(ISSUER, "eshop-api", "other-client", future)).hasErrors()).isTrue();
        assertThat(validator.validate(token(ISSUER, "eshop-api", "eshop-web", Instant.now().minusSeconds(120))).hasErrors()).isTrue();
    }
    @Test void forgedTokenCannotLookupIdentityOrIssueSession() {
        JwtDecoder decoder = mock(JwtDecoder.class);
        var identities = mock(ExternalIdentityRepository.class);
        var auth = mock(AuthServiceImpl.class);
        var audit = mock(AuditLogService.class);
        when(decoder.decode("forged")).thenThrow(new BadJwtException("bad signature"));
        var service = new KeycloakExchangeService(decoder, identities, auth, audit, true);
        assertThatThrownBy(() -> service.exchange("forged")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verifyNoInteractions(identities, auth);
    }
    @Test void noAutomaticEmailOrRoleLinking() {
        JwtDecoder decoder = mock(JwtDecoder.class);
        var identities = mock(ExternalIdentityRepository.class);
        var auth = mock(AuthServiceImpl.class);
        var audit = mock(AuditLogService.class);
        when(decoder.decode("valid")).thenReturn(token(ISSUER, "eshop-api", "eshop-web", Instant.now().plusSeconds(300)));
        when(identities.findByIssuerAndSubject(ISSUER, "subject-id")).thenReturn(Optional.empty());
        var service = new KeycloakExchangeService(decoder, identities, auth, audit, true);
        assertThatThrownBy(() -> service.exchange("valid")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verifyNoInteractions(auth);
    }
    @Test void disabledIntegrationNeverReadsTokens() {
        JwtDecoder decoder = mock(JwtDecoder.class);
        var service = new KeycloakExchangeService(decoder, mock(ExternalIdentityRepository.class), mock(AuthServiceImpl.class), mock(AuditLogService.class), false);
        assertThatThrownBy(() -> service.exchange("anything")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verifyNoInteractions(decoder);
    }
}
