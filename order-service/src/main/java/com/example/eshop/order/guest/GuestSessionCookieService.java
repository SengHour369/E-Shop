package com.example.eshop.order.guest;

import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;
import java.time.Duration;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class GuestSessionCookieService {
    public static final String COOKIE = "eshop_guest";
    private final SecureRandom random = new SecureRandom();
    private final boolean secure;
    private final Set<String> origins;
    public GuestSessionCookieService(@Value("${guest.cookie-secure:true}") boolean secure,
            @Value("${guest.allowed-origins:http://localhost:4200}") Set<String> origins) {
        this.secure=secure; this.origins=Set.copyOf(origins);
    }
    public String randomToken() { byte[] bytes=new byte[32]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    public static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public String read(HttpServletRequest request) {
        String token=null;
        if(request.getCookies()!=null) for(var cookie:request.getCookies()) if(COOKIE.equals(cookie.getName())) {
            if(token!=null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            token=cookie.getValue();
        }
        if(token!=null&&!token.matches("[A-Za-z0-9_-]{43}")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return token;
    }
    public void requireOrigin(HttpServletRequest request) {
        if(java.util.Collections.list(request.getHeaders("Origin")).size()!=1 || !origins.contains(request.getHeader("Origin"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Untrusted origin");
        if("cross-site".equals(request.getHeader("Sec-Fetch-Site"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    public void set(HttpServletResponse response,String token,Duration ttl) {
        response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from(COOKIE,token).httpOnly(true).secure(secure)
                .sameSite("Lax").path("/api/v1/guest").maxAge(ttl).build().toString());
        response.setHeader(HttpHeaders.CACHE_CONTROL,"no-store");
    }
}
