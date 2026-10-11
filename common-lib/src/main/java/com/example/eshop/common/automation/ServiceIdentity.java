package com.example.eshop.common.automation;
import com.example.eshop.common.jwt.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
public final class ServiceIdentity {
    private ServiceIdentity() {}
    public static String bearer(JwtProperties jwt, String service, String authority) {
        Instant now = Instant.now();
        return "Bearer " + Jwts.builder().setSubject(service).claim("authorities", List.of(authority))
                .claim("isEnable", true).setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwt.getSecret()))).compact();
    }
}
