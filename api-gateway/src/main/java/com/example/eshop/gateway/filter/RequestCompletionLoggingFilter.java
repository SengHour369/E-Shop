package com.example.eshop.gateway.filter;

import com.example.eshop.gateway.config.GatewayLoggingProperties;
import com.example.eshop.gateway.logging.GatewayRequestLog;
import com.example.eshop.gateway.repository.logging.GatewayRequestLogRepository;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Logs one completion event per request without reading or retaining request/response bodies. */

@Component
public class RequestCompletionLoggingFilter implements GlobalFilter, Ordered {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RequestCompletionLoggingFilter.class);

  private final GatewayRequestLogRepository repository;
  private final GatewayLoggingProperties properties;

  public RequestCompletionLoggingFilter(
      GatewayRequestLogRepository repository, GatewayLoggingProperties properties) {
    this.repository = repository;
    this.properties = properties;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    long startedAt = System.nanoTime();
    return chain.filter(exchange)
        .then(Mono.defer(() -> complete(exchange, startedAt)))
        .onErrorResume(error -> complete(exchange, startedAt).then(Mono.error(error)));
  }

  private Mono<Void> complete(ServerWebExchange exchange, long startedAt) {
    long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    HttpStatusCode status = exchange.getResponse().getStatusCode();
    String correlationId =
        exchange.getAttributeOrDefault(GatewayRequestHeaders.CORRELATION_ID_ATTRIBUTE, "unknown");

    try (var ignored = org.slf4j.MDC.putCloseable("requestId", correlationId)) {
    log.info(
        "gateway_request requestId={} method={} path={} status={} durationMs={}",
        correlationId,
        exchange.getRequest().getMethod(),
        exchange.getRequest().getPath().value(),
        status == null ? "unknown" : status.value(),
        durationMs);
    }

    if (!properties.isDatabaseEnabled()) {
      return Mono.empty();
    }

    GatewayRequestLog requestLog = new GatewayRequestLog();
    requestLog.setCorrelationId(correlationId);
    requestLog.setMethod(exchange.getRequest().getMethod().name());
    requestLog.setPath(exchange.getRequest().getPath().value());
    requestLog.setResponseStatus(status == null ? null : status.value());
    requestLog.setDurationMs(durationMs);
    requestLog.setClientIp(remoteAddress(exchange));
    requestLog.setCreatedAt(Instant.now());
    return repository
        .save(requestLog)
        .timeout(java.time.Duration.ofSeconds(1))
        .doOnError(error -> log.warn("Gateway request log write failed: {}", error.getMessage()))
        .onErrorResume(error -> Mono.empty())
        .then();
  }

  private String remoteAddress(ServerWebExchange exchange) {
    var address = exchange.getRequest().getRemoteAddress();
    return address == null ? null : address.getAddress().getHostAddress();
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 2;
  }
}
