package com.example.eshop.gateway.service.ratelimit;

import com.example.eshop.gateway.repository.route.GatewayRouteRepository;
import com.example.eshop.gateway.route.GatewayRoute;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Flux;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ReactiveRateLimiterServiceTest {
  @Test void redisDenialBlocksMatchingPolicyAndOtherMethodsAreUnrestricted() {
    var repository = mock(GatewayRouteRepository.class);
    var redis = mock(ReactiveStringRedisTemplate.class);
    var route = new GatewayRoute();
    route.setRouteKey("orders");
    route.setPathPattern("/api/v1/orders/**");
    route.setHttpMethod("POST");
    route.setRateLimit(2);
    route.setRateLimitWindowSeconds(60);
    when(repository.findByEnabledTrue()).thenReturn(Flux.just(route));
    when(redis.execute(any(RedisScript.class), eq(List.of("gateway:rate:orders:shopper")), eq(List.of("2", "60"))))
        .thenReturn(Flux.just(0L));
    var service = new ReactiveRateLimiterService(repository, redis);
    assertThat(service.allow("/api/v1/orders/create", HttpMethod.POST, "shopper").block()).isFalse();
    assertThat(service.allow("/api/v1/orders/create", HttpMethod.GET, "shopper").block()).isTrue();
    verify(repository, times(1)).findByEnabledTrue();
    service.refreshPolicies();
    service.allow("/api/v1/orders/create", HttpMethod.GET, "shopper").block();
    verify(repository, times(2)).findByEnabledTrue();
  }

  @Test void unavailablePolicyDatabaseUsesReferenceFailOpenBehavior() {
    var repository = mock(GatewayRouteRepository.class);
    var redis = mock(ReactiveStringRedisTemplate.class);
    when(repository.findByEnabledTrue()).thenReturn(Flux.error(new IllegalStateException("offline")));
    assertThat(new ReactiveRateLimiterService(repository, redis)
        .allow("/api/v1/orders", HttpMethod.GET, "shopper").block()).isTrue();
    verifyNoInteractions(redis);
  }
}
