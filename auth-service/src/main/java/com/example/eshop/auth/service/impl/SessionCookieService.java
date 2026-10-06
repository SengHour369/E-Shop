package com.example.eshop.auth.service.impl;

import com.example.eshop.auth.dto.response.AuthenticationResponse;
import java.time.Duration;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Cookie transport only; credential checks and refresh rotation stay in AuthServiceImpl. */
@Service
public class SessionCookieService {
    public static final String ACCESS_COOKIE = "eshop_access";
    public static final String REFRESH_COOKIE = "eshop_refresh";
    public static final String SESSION_PATH = "/api/v1/public/session";
    private final boolean secure;
    private final String sameSite;
    private final long accessSeconds;
    private final Set<String> allowedOrigins;

    public SessionCookieService(
            @Value("${auth.cookies.secure:true}") boolean secure,
            @Value("${auth.cookies.same-site:Lax}") String sameSite,
            @Value("${jwt.expiration}") long accessSeconds,
            @Value("${auth.cookies.allowed-origins}") Set<String> allowedOrigins) {
        this.secure = secure;
        this.sameSite = java.util.Arrays.stream(new String[]{"Lax", "Strict", "None"})
                .filter(value -> value.equalsIgnoreCase(sameSite.trim())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cookie SameSite must be Lax, Strict or None"));
        if (this.sameSite.equals("None") && !secure) {
            throw new IllegalArgumentException("SameSite=None requires secure HTTPS cookies");
        }
        this.accessSeconds = accessSeconds;
        this.allowedOrigins = Set.copyOf(allowedOrigins);
    }

    public void requireTrustedOrigin(String origin) {
        if (origin == null || !allowedOrigins.contains(origin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Untrusted or missing browser Origin");
        }
    }

    public HttpHeaders issue(AuthenticationResponse tokens) {
        HttpHeaders headers = noStore();
        headers.add(HttpHeaders.SET_COOKIE, cookie(ACCESS_COOKIE, tokens.accessToken(), "/api", accessSeconds));
        headers.add(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, tokens.refreshToken(), SESSION_PATH, 604800));
        return headers;
    }

    public HttpHeaders clear() {
        HttpHeaders headers = noStore();
        headers.add(HttpHeaders.SET_COOKIE, cookie(ACCESS_COOKIE, "", "/api", 0));
        headers.add(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, "", SESSION_PATH, 0));
        return headers;
    }

    private HttpHeaders noStore() {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl("no-store");
        return headers;
    }

    private String cookie(String name, String value, String path, long seconds) {
        return ResponseCookie.from(name, value).httpOnly(true).secure(secure).sameSite(sameSite)
                .path(path).maxAge(Duration.ofSeconds(seconds)).build().toString();
    }

    public AuthenticationResponse withoutTokens(AuthenticationResponse response) {
        return new AuthenticationResponse(response.id(), null, null, "Cookie", response.email(),
                response.username(), response.role(), response.message());
    }
}
