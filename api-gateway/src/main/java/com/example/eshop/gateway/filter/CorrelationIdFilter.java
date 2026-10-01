package com.example.eshop.gateway.filter;

import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gives every gateway request one safe correlation ID and propagates it in both directions. Clients
 * may supply their own ID when it uses the supported log-safe format.
 */
@Component
public class CorrelationIdFilter implements WebFilter, Ordered {

  private static final int MAX_CORRELATION_ID_LENGTH = 128;
  private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("[A-Za-z0-9._:-]+");

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    var headers = exchange.getRequest().getHeaders();
    var values = headers.getOrEmpty("X-Request-ID");
    if (values.isEmpty()) values = headers.getOrEmpty(GatewayRequestHeaders.CORRELATION_ID);
    String correlationId = resolveCorrelationId(values.size() == 1 ? values.get(0) : null);

    exchange.getAttributes().put(GatewayRequestHeaders.CORRELATION_ID_ATTRIBUTE, correlationId);
    exchange.getResponse().getHeaders().set(GatewayRequestHeaders.CORRELATION_ID, correlationId);

    exchange.getResponse().getHeaders().set("X-Request-ID", correlationId);
    exchange.getResponse().beforeCommit(() -> {
      exchange.getResponse().getHeaders().set("X-Request-ID", correlationId);
      exchange.getResponse().getHeaders().set(GatewayRequestHeaders.CORRELATION_ID, correlationId);
      return Mono.empty();
    });
    ServerHttpRequest request =
        exchange
            .getRequest()
            .mutate()
            .headers(outgoing -> { outgoing.set(GatewayRequestHeaders.CORRELATION_ID, correlationId); outgoing.set("X-Request-ID", correlationId); })
            .build();

    return chain.filter(exchange.mutate().request(request).build()).contextWrite(context -> context.put("requestId", correlationId));
  }

  private String resolveCorrelationId(String suppliedValue) {
    if (StringUtils.hasText(suppliedValue)
        && suppliedValue.length() <= MAX_CORRELATION_ID_LENGTH
        && SAFE_CORRELATION_ID.matcher(suppliedValue).matches()) {
      return suppliedValue;
    }
    return UUID.randomUUID().toString();
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
