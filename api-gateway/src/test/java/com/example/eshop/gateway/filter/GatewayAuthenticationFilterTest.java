package com.example.eshop.gateway.filter;

import com.example.eshop.gateway.config.GatewaySecurityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import static org.assertj.core.api.Assertions.assertThat;

class GatewayAuthenticationFilterTest {
  private static final String SECRET = "dGhpcy1pcy1hLXZlcnktc2VjdXJlLXNlY3JldC1rZXktZm9yLWp3dC10b2tlbg==";
  private GatewaySecurityProperties properties;
  private GatewayAuthenticationFilter filter;

  @BeforeEach void setup() {
    properties = new GatewaySecurityProperties();
    properties.setAuthenticationEnabled(true);
    properties.setJwtSecret(SECRET);
    properties.setPublicPaths(List.of("/api/v1/public/email/username/login"));
    filter = new GatewayAuthenticationFilter(properties, new ObjectMapper());
  }

  private String token(boolean refresh, boolean enabled, long seconds) {
    return Jwts.builder().subject("shopper@example.com").claim("isEnable", enabled)
        .claim("type", refresh ? "refresh" : "access")
        .expiration(Date.from(Instant.now().plusSeconds(seconds)))
        .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)), Jwts.SIG.HS256).compact();
  }

  @Test void forwardsUsernameWithoutInventingNumericUserId() {
    var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders")
        .header("Authorization", "Bearer " + token(false, true, 60))
        .header("X-User-Id", "999").header("X-Authenticated-Username", "attacker"));
    var forwarded = new AtomicReference<ServerWebExchange>();
    filter.filter(exchange, current -> { forwarded.set(current); return current.getResponse().setComplete(); }).block();
    assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-Authenticated-Username"))
        .isEqualTo("shopper@example.com");
    assertThat(forwarded.get().getRequest().getHeaders()).doesNotContainKey("X-User-Id");
  }

  @Test void rejectsInvalidExpiredRefreshAndDisabledAccountTokens() {
    for (String value : List.of("invalid", token(true, true, 60), token(false, true, -60), token(false, false, 60))) {
      var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders")
          .header("Authorization", "Bearer " + value));
      filter.filter(exchange, current -> { throw new AssertionError("Must not forward"); }).block();
      assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Test void publicLoginStripsSpoofedIdentityAndAdminHeaders() {
    var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/public/email/username/login")
        .header("X-User-Id", "999").header("X-Gateway-Admin-Key", "secret"));
    var forwarded = new AtomicReference<ServerWebExchange>();
    filter.filter(exchange, current -> { forwarded.set(current); return current.getResponse().setComplete(); }).block();
    assertThat(forwarded.get().getRequest().getHeaders()).doesNotContainKeys("X-User-Id", "X-Gateway-Admin-Key");
  }

  @Test void loginSuffixAndMissingTokenAreNotPublic() {
    var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/public/email/username/login/admin"));
    filter.filter(exchange, current -> { throw new AssertionError("Must not forward"); }).block();
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test void cookieRequestForwardsBearerTokenButNotCookies() {
    String access = token(false, true, 60);
    var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/orders")
        .header("Origin", "http://localhost:5173")
        .cookie(new org.springframework.http.HttpCookie("eshop_access", access)));
    var forwarded = new AtomicReference<ServerWebExchange>();
    filter.filter(exchange, current -> { forwarded.set(current); return current.getResponse().setComplete(); }).block();
    assertThat(forwarded.get().getRequest().getHeaders().getFirst("Authorization")).isEqualTo("Bearer " + access);
    assertThat(forwarded.get().getRequest().getHeaders()).doesNotContainKey("Cookie");
  }

  @Test void unsafeCookieRequestsRejectMissingAndUntrustedOrigins() {
    for (String origin : List.of("", "https://attacker.example")) {
      var builder = MockServerHttpRequest.post("/api/v1/orders")
          .cookie(new org.springframework.http.HttpCookie("eshop_access", token(false, true, 60)));
      if (!origin.isEmpty()) builder.header("Origin", origin);
      var exchange = MockServerWebExchange.from(builder);
      filter.filter(exchange, current -> { throw new AssertionError("Must not forward"); }).block();
      assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
  }

  @Test void invalidExplicitBearerDoesNotFallBackToValidCookie() {
    var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders")
        .header("Authorization", "Bearer invalid")
        .cookie(new org.springframework.http.HttpCookie("eshop_access", token(false, true, 60))));
    filter.filter(exchange, current -> { throw new AssertionError("Must not forward"); }).block();
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }
}
