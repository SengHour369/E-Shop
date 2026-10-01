package com.example.eshop.gateway.service.ratelimit;

import com.example.eshop.gateway.route.GatewayRoute;
import com.example.eshop.gateway.repository.route.GatewayRouteRepository;
import java.util.List;
import java.time.Duration;
import org.springframework.context.event.EventListener;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.web.util.pattern.PathPattern;
import reactor.core.publisher.Flux;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Service;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;


@Service
public class ReactiveRateLimiterService {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReactiveRateLimiterService.class);
  @org.springframework.beans.factory.annotation.Value("${gateway.dynamic-routes-enabled:true}")
  private boolean dynamicRoutesEnabled = true;
  private static final RedisScript<Long> SCRIPT =
      RedisScript.of(
          """
          local current = redis.call('INCR', KEYS[1])
          if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[2]) end
          if current > tonumber(ARGV[1]) then return 0 end
          return 1
          """,
          Long.class);

  private final GatewayRouteRepository routes;
  private final ReactiveStringRedisTemplate redis;
  private final PathPatternParser patterns = new PathPatternParser();
  private volatile Mono<List<Policy>> policies;

  private record Policy(GatewayRoute route, PathPattern path) {}

  public ReactiveRateLimiterService(
      GatewayRouteRepository routes, ReactiveStringRedisTemplate redis) {
    this.routes = routes;
    this.redis = redis;
    refreshPolicies();
  }

  /** Share one asynchronous load across requests; route edits invalidate immediately. */
  @EventListener(RefreshRoutesEvent.class)
  public void refreshPolicies() {
    policies = Flux.defer(routes::findByEnabledTrue)
        .filter(route -> route.getRateLimit() != null && route.getRateLimitWindowSeconds() != null)
        .map(route -> new Policy(route, patterns.parse(route.getPathPattern())))
        .collectList().timeout(Duration.ofSeconds(1))
        .cache(Duration.ofSeconds(30));
  }

  public Mono<Boolean> allow(String path, HttpMethod method, String identifier) {
    if (!dynamicRoutesEnabled) {
      return Mono.just(true);
    }
    return policies.flatMapMany(Flux::fromIterable)
        .filter(policy -> applies(policy, path, method))
        .next()
        .flatMap(policy -> enforce(policy.route(), identifier))
        .defaultIfEmpty(true)
        .onErrorResume(
            error -> {
              log.warn("Rate limiter unavailable; allowing request: {}", error.getMessage());
              return Mono.just(true);
            });
  }

  private boolean applies(Policy policy, String path, HttpMethod method) {
    GatewayRoute route = policy.route();
    boolean pathMatches =
        policy.path().matches(PathContainer.parsePath(path));
    boolean methodMatches =
        route.getHttpMethod() == null
            || route.getHttpMethod().isBlank()
            || route.getHttpMethod().equals(method.name());
    return pathMatches
        && methodMatches
        && route.getRateLimit() != null
        && route.getRateLimitWindowSeconds() != null;
  }

  private Mono<Boolean> enforce(GatewayRoute route, String identifier) {
    String key = "gateway:rate:" + route.getRouteKey() + ":" + identifier;
    return redis
        .execute(
            SCRIPT,
            List.of(key),
            List.of(route.getRateLimit().toString(), route.getRateLimitWindowSeconds().toString()))
        .next()
        .timeout(Duration.ofSeconds(1))
        .map(result -> result != null && result == 1L);
  }
}
