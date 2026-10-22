package com.example.eshop.order.guest;

import com.example.eshop.common.automation.ServiceIdentity;
import com.example.eshop.common.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestClient;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;

@Service @RequiredArgsConstructor
public class GuestContactService {
    private final StringRedisTemplate redis;
    private final GuestRateLimiter limiter;
    private final GuestSessionRepository sessions;
    private final JwtProperties jwt;
    @Value("${authorization.auth-url:http://localhost:8081}") private String authUrl;
    private final SecureRandom random=new SecureRandom();
    public void send(GuestSession session,String email) {
        String normalized=email.trim().toLowerCase(Locale.ROOT);
        limiter.require("contact-session",session.getId().toString(),3,900);
        limiter.require("contact-email",normalized,3,3600);
        String code=String.format(Locale.ROOT,"%08d",random.nextInt(100_000_000));
        String key="guest:contact:"+session.getId();
        // Replace the previous challenge before delivery, so old codes cannot validate a new email.
        redis.opsForValue().set(key,normalized+"\n"+GuestSessionCookieService.hash(session.getId()+":"+code),Duration.ofMinutes(5));
        var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory();factory.setConnectTimeout(2000);factory.setReadTimeout(10000);
        try {
            RestClient.builder().baseUrl(authUrl).requestFactory(factory).build().post().uri("/internal/commerce/guest/contact-code")
                    .header("Authorization",ServiceIdentity.bearer(jwt,"order-service","SERVICE_ORDER"))
                    .body(Map.of("email",normalized,"code",code)).retrieve().toBodilessEntity();
        } catch(Exception e) { redis.delete(key);throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Contact verification delivery unavailable"); }
    }
    @Transactional
    public void verify(GuestSession session,String email,String code) {
        limiter.require("contact-attempts",session.getId().toString(),5,300);
        String key="guest:contact:"+session.getId();String challenge=redis.opsForValue().get(key);
        String expected=email.trim().toLowerCase(Locale.ROOT)+"\n"+GuestSessionCookieService.hash(session.getId()+":"+code);
        if(challenge==null||!MessageDigest.isEqual(challenge.getBytes(StandardCharsets.UTF_8),expected.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid or expired contact code");
        // Atomic consume rejects concurrent replay.
        Long consumed=redis.execute(new org.springframework.data.redis.core.script.DefaultRedisScript<>(
                "if redis.call('GET',KEYS[1]) == ARGV[1] then return redis.call('DEL',KEYS[1]) else return 0 end",Long.class),List.of(key),expected);
        if(!Long.valueOf(1).equals(consumed)) throw new ResponseStatusException(HttpStatus.CONFLICT);
        var current=sessions.lockById(session.getId()).filter(s->s.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        current.setVerifiedEmail(email.trim().toLowerCase(Locale.ROOT));current.setContactVerifiedAt(Instant.now());
    }
}
