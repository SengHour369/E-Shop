package com.example.eshop.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;

/**
 * Stateless JWT verification shared by every downstream service. Only auth-service
 * issues tokens (it has the user store); everyone else just needs to trust the signature,
 * so this never hits a database.
 */
@Component
public class JwtTokenValidator {

    private final JwtProperties properties;

    public JwtTokenValidator(JwtProperties properties) {
        this.properties = properties;
    }

    private Key getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(properties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
