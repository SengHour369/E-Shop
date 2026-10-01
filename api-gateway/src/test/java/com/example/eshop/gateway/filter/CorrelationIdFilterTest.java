package com.example.eshop.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class CorrelationIdFilterTest {

  private final CorrelationIdFilter filter = new CorrelationIdFilter();

  @Test
  void preservesSafeClientCorrelationIdAndForwardsIt() {
    var exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/products")
                .header(GatewayRequestHeaders.CORRELATION_ID, "client-request-42"));
    var forwardedExchange = new AtomicReference<org.springframework.web.server.ServerWebExchange>();

    filter
        .filter(
            exchange,
            current -> {
              forwardedExchange.set(current);
              current.getResponse().setStatusCode(HttpStatus.OK);
              return current.getResponse().setComplete();
            })
        .block();

    assertThat(forwardedExchange.get()).isNotNull();
    assertThat(
            forwardedExchange
                .get()
                .getRequest()
                .getHeaders()
                .getFirst(GatewayRequestHeaders.CORRELATION_ID))
        .isEqualTo("client-request-42");
    assertThat(exchange.getResponse().getHeaders().getFirst(GatewayRequestHeaders.CORRELATION_ID))
        .isEqualTo("client-request-42");
  }

  @Test
  void replacesUnsafeCorrelationId() {
    var exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/products")
                .header(GatewayRequestHeaders.CORRELATION_ID, "unsafe value with spaces"));
    var forwardedId = new AtomicReference<String>();

    filter
        .filter(
            exchange,
            current -> {
              forwardedId.set(
                  current.getRequest().getHeaders().getFirst(GatewayRequestHeaders.CORRELATION_ID));
              return current.getResponse().setComplete();
            })
        .block();

    assertThat(forwardedId.get())
        .isNotBlank()
        .isNotEqualTo("unsafe value with spaces")
        .matches("[0-9a-f-]{36}");
  }

  @Test void preferredIdWinsAndDuplicateIdsAreReplaced() {
    var request = MockServerHttpRequest.get("/missing")
        .header("X-Request-ID", "req_preferred").header(GatewayRequestHeaders.CORRELATION_ID, "legacy");
    var exchange = MockServerWebExchange.from(request);
    filter.filter(exchange, current -> {
      assertThat(current.getRequest().getHeaders().getFirst("X-Request-ID")).isEqualTo("req_preferred");
      return current.getResponse().setComplete();
    }).block();
    assertThat(exchange.getResponse().getHeaders().getFirst("X-Request-ID")).isEqualTo("req_preferred");
    var duplicate = MockServerWebExchange.from(MockServerHttpRequest.get("/")
        .header("X-Request-ID", "one", "two"));
    filter.filter(duplicate, current -> current.getResponse().setComplete()).block();
    assertThat(duplicate.getResponse().getHeaders().getFirst("X-Request-ID")).isNotIn("one", "two");
  }
}
