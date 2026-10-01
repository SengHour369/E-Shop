package com.example.eshop.gateway.filter;

import java.net.InetSocketAddress;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Forwards transport-level client metadata to downstream services without trusting spoofed IPs. */
@Component
public class ClientMetadataFilter implements GlobalFilter, Ordered {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String clientIp = remoteAddress(exchange.getRequest().getRemoteAddress());
    ServerHttpRequest request =
        exchange
            .getRequest()
            .mutate()
            .headers(
                headers -> {
                  headers.remove("Forwarded");
                  headers.remove("X-Forwarded-For");
                  headers.remove("X-Real-IP");
                  if (clientIp != null) {
                    headers.set("X-Forwarded-For", clientIp);
                    headers.set("X-Real-IP", clientIp);
                  }
                })
            .build();
    return chain.filter(exchange.mutate().request(request).build());
  }

  private String remoteAddress(InetSocketAddress address) {
    if (address == null || address.getAddress() == null) {
      return null;
    }
    return address.getAddress().getHostAddress();
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 1;
  }
}
