package com.example.eshop.order.guest;

import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

@Service @RequiredArgsConstructor
public class GuestSessionService {
    private static final Duration LIFETIME=Duration.ofDays(7);
    private final GuestSessionRepository repository;
    private final GuestSessionCookieService cookies;
    private final GuestRateLimiter limiter;
    public record SessionView(String csrfToken,Instant expiresAt) { }
    @Transactional
    public SessionView establish(HttpServletRequest request,HttpServletResponse response) {
        cookies.requireOrigin(request);
        // The direct peer is trustworthy; do not accept browser-supplied forwarded addresses.
        limiter.require("session",request.getRemoteAddr(),10000,3600);
        String raw=cookies.read(request);
        GuestSession session=raw==null?null:repository.findByTokenHash(GuestSessionCookieService.hash(raw)).orElse(null);
        Instant now=Instant.now();
        if(session==null||!session.getExpiresAt().isAfter(now)) {
            session=new GuestSession(); session.setExpiresAt(now.plus(LIFETIME));
        }
        // Rotate both secrets at bootstrap; retain stable server-side ownership and absolute expiry.
        String token=cookies.randomToken(),csrf=cookies.randomToken();
        session.setTokenHash(GuestSessionCookieService.hash(token)); session.setCsrfHash(GuestSessionCookieService.hash(csrf));
        session.setRotatedAt(now); repository.saveAndFlush(session);
        cookies.set(response,token,Duration.between(now,session.getExpiresAt()));
        return new SessionView(csrf,session.getExpiresAt());
    }
    @Transactional
    public GuestSession require(HttpServletRequest request,boolean mutation) {
        String token=cookies.read(request);
        if(token==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Guest session expired");
        var session=repository.findByTokenHash(GuestSessionCookieService.hash(token))
                .filter(s->s.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Guest session expired"));
        limiter.require("activity",session.getId().toString(),120,60);
        if(mutation) {
            cookies.requireOrigin(request);
            String csrf=request.getHeader("X-Guest-CSRF");
            if(csrf==null||!csrf.matches("[A-Za-z0-9_-]{43}")||!MessageDigest.isEqual(
                    session.getCsrfHash().getBytes(StandardCharsets.US_ASCII),
                    GuestSessionCookieService.hash(csrf).getBytes(StandardCharsets.US_ASCII)))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Guest CSRF verification failed");
        }
        return session;
    }
    @Transactional
    public void revoke(HttpServletRequest request,HttpServletResponse response) {
        var session=require(request,true); session.setExpiresAt(Instant.now());
        cookies.set(response,"",Duration.ZERO);
    }
}
