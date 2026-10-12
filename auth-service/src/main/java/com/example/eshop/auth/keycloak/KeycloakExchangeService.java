package com.example.eshop.auth.keycloak;

import com.example.eshop.auth.dto.response.AuthenticationResponse;
import com.example.eshop.auth.service.impl.AuthServiceImpl;
import com.example.eshop.common.audit.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class KeycloakExchangeService {
    private final JwtDecoder decoder;
    private final ExternalIdentityRepository identities;
    private final AuthServiceImpl auth;
    private final AuditLogService audit;
    private final boolean enabled;
    public KeycloakExchangeService(@Qualifier("keycloakJwtDecoder") JwtDecoder decoder,
            ExternalIdentityRepository identities, AuthServiceImpl auth, AuditLogService audit,
            @Value("${eshop.keycloak.enabled:false}") boolean enabled) {
        this.decoder = decoder; this.identities = identities; this.auth = auth; this.audit = audit; this.enabled = enabled;
    }
    @Transactional
    public AuthenticationResponse exchange(String token) {
        if (!enabled) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Keycloak is disabled");
        try {
            Jwt jwt = decoder.decode(token);
            var identity = identities.findByIssuerAndSubject(jwt.getIssuer().toString(), jwt.getSubject())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identity is not linked"));
            return auth.authenticateExternal(identity.getUser());
        } catch (JwtException | ResponseStatusException ex) {
            audit.recordSecurity(AuditAction.LOGIN_FAILED, AuditResult.FAILURE, null, "KEYCLOAK_LOGIN_REJECTED");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Keycloak sign-in rejected");
        }
    }
}
