package com.example.eshop.catalog.scanner;

import com.example.eshop.common.request.RequestIds;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import static org.assertj.core.api.Assertions.assertThat;

class HttpProductVisionClientTest {
    @Test void requestIdIsForwardedAndCatalogIdsFromTheModelAreIgnored() throws Exception {
        AtomicReference<String> requestId = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/ai/vision/products", exchange -> {
            requestId.set(exchange.getRequestHeaders().getFirst("X-Request-ID"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    {"candidates":[{"name":"Nike Air Max","confidence":0.8,"productId":999}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            MDC.put("requestId", "req_vision");
            var client = new HttpProductVisionClient(new ObjectMapper(),
                    "http://127.0.0.1:" + server.getAddress().getPort(), Duration.ofSeconds(2));
            VisionOutcome outcome = client.identify(new byte[]{1, 2, 3}, "image/jpeg");
            assertThat(requestId.get()).isEqualTo("req_vision");
            assertThat(body.get()).doesNotContain("Bearer");
            assertThat(outcome.available()).isTrue();
            assertThat(outcome.labels()).containsExactly(new VisionLabel("Nike Air Max", 0.8));
        } finally {
            MDC.clear();
            server.stop(0);
        }
    }

    @Test void providerFailureReturnsNoLabels() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/ai/vision/products", exchange -> {
            byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(503, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            var client = new HttpProductVisionClient(new ObjectMapper(),
                    "http://127.0.0.1:" + server.getAddress().getPort(), Duration.ofSeconds(2));
            assertThat(client.identify(new byte[]{1}, "image/jpeg").available()).isFalse();
        } finally {
            server.stop(0);
        }
    }
}
