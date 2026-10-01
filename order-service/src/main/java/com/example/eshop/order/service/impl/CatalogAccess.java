package com.example.eshop.order.service.impl;
import com.example.eshop.common.dto.*;
import com.example.eshop.common.jwt.JwtProperties;
import com.example.eshop.order.client.CatalogCheckoutClient;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;
@Service @RequiredArgsConstructor
public class CatalogAccess {
    private final CatalogCheckoutClient client;
    private final JwtProperties jwt;
    private String token() {
        Instant now = Instant.now();
        return "Bearer " + Jwts.builder().setSubject("order-service")
            .claim("authorities", List.of("SERVICE_ORDER")).claim("isEnable", true)
            .setIssuedAt(Date.from(now)).setExpiration(Date.from(now.plusSeconds(60)))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwt.getSecret()))).compact();
    }
    public CheckoutResult quote(Long userId, Long skuId, Long quantity) {
        return client.quote(token(), new CheckoutRequest(1L, userId, List.of(new CheckoutRequest.Line(skuId, quantity))));
    }
    public CheckoutResult reserve(CheckoutRequest request) { return client.reserve(token(), request); }
    public CheckoutResult confirm(Long id) { return client.confirm(token(), id); }
    public void release(Long id) { client.release(token(), id); }
}

