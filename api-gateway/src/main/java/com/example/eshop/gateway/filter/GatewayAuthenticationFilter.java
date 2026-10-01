package com.example.eshop.gateway.filter;

import com.example.eshop.gateway.config.GatewaySecurityProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Map;
import java.util.Set;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayAuthenticationFilter implements GlobalFilter, Ordered {
  private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
  private final GatewaySecurityProperties properties;
  private final ObjectMapper objectMapper;
  private final JwtParser parser;

  public GatewayAuthenticationFilter(
      GatewaySecurityProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.parser = properties.isAuthenticationEnabled()
        ? Jwts.parser().verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.getJwtSecret()))).build()
        : null;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    var cookie = exchange.getRequest().getCookies().getFirst("eshop_access");
    boolean cookieAuthentication = authorization == null && cookie != null;
    boolean sessionEndpoint = exchange.getRequest().getPath().value().startsWith("/api/v1/public/session/");
    String method = exchange.getRequest().getMethod().name();
    boolean unsafe = !SAFE_METHODS.contains(method);
    String origin = exchange.getRequest().getHeaders().getOrigin();
    if (unsafe && (cookieAuthentication || sessionEndpoint)
        && (origin == null || !properties.getCookieAllowedOrigins().contains(origin))) {
      exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
      return exchange.getResponse().setComplete();
    }
    var sanitized = exchange.getRequest().mutate().headers(headers -> {
      headers.remove(GatewayRequestHeaders.AUTHENTICATED_USERNAME);
      headers.remove("X-Authenticated-User-Id");
      headers.remove("X-User-Id");
      headers.remove("X-Actor-User-Id");
      headers.remove("X-Gateway-Admin-Key");
    }).build();
    exchange = exchange.mutate().request(sanitized).build();
    if (!properties.isAuthenticationEnabled()
        || isPublic(exchange.getRequest().getPath().value())) {
      return chain.filter(exchange);
    }

    if (cookieAuthentication) {
      authorization = "Bearer " + cookie.getValue();
    }
    if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
      return unauthorized(exchange, "Missing or invalid Authorization header");
    }

    try {
      String token = authorization.substring(7);
      String username = parseSubject(token);
      exchange.getAttributes().put(GatewayRequestHeaders.AUTHENTICATED_USERNAME_ATTRIBUTE, username);
      var request =
          exchange
              .getRequest()
              .mutate()
              .headers(
                  headers -> {
                    headers.set(GatewayRequestHeaders.AUTHENTICATED_USERNAME, username);
                    headers.setBearerAuth(token);
                    headers.remove(HttpHeaders.COOKIE);

                  })
              .build();
      return chain.filter(exchange.mutate().request(request).build());
    } catch (JwtException | IllegalArgumentException exception) {
      return unauthorized(exchange, "Invalid or expired access token");
    }
  }

  private boolean isPublic(String path) {
    return properties.getPublicPaths().stream()
        .anyMatch(
            pattern ->
                pattern.endsWith("/**")
                    ? path.startsWith(pattern.substring(0, pattern.length() - 2))
                    : path.equals(pattern));
  }


  private String parseSubject(String token) {
    var claims = parser.parseSignedClaims(token).getPayload();
    if (claims.getExpiration() == null || "refresh".equals(claims.get("type"))
        || !Boolean.TRUE.equals(claims.get("isEnable", Boolean.class))) {
      throw new IllegalArgumentException("An enabled account access token is required");
    }
    String subject = claims.getSubject();
    if (!StringUtils.hasText(subject)) {
      throw new IllegalArgumentException("JWT subject is missing");
    }
    return subject;
  }

  private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    try {
      byte[] body =
          objectMapper.writeValueAsBytes(
              Map.of(
                  "requestId", exchange.getAttributeOrDefault(GatewayRequestHeaders.CORRELATION_ID_ATTRIBUTE, "unknown"),
                  "status",
                  HttpStatus.UNAUTHORIZED.value(),
                  "error",
                  "UNAUTHORIZED",
                  "message",
                  message));
      DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
      return exchange.getResponse().writeWith(Mono.just(buffer));
    } catch (JsonProcessingException exception) {
      return exchange.getResponse().setComplete();
    }
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 5;
  }
}
