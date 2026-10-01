package com.example.eshop.gateway.filter;

import com.example.eshop.gateway.service.ratelimit.ReactiveRateLimiterService;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {
  private final ReactiveRateLimiterService rateLimiter;

  public RateLimitFilter(ReactiveRateLimiterService rateLimiter) {
    this.rateLimiter = rateLimiter;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String identifier =
        exchange.getAttributeOrDefault(
            GatewayRequestHeaders.AUTHENTICATED_USERNAME_ATTRIBUTE, remoteAddress(exchange));
    return rateLimiter
        .allow(
            exchange.getRequest().getPath().value(), exchange.getRequest().getMethod(), identifier)
        .flatMap(
            allowed -> {
              if (allowed) {
                return chain.filter(exchange);
              }
              exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
              exchange.getResponse().getHeaders().set("Retry-After", "1");
              return exchange.getResponse().setComplete();
            });
  }

  private String remoteAddress(ServerWebExchange exchange) {
    var address = exchange.getRequest().getRemoteAddress();
    return address == null ? "unknown" : address.getAddress().getHostAddress();
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 20;
  }
}
