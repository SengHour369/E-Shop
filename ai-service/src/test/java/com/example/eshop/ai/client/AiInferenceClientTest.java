package com.example.eshop.ai.client;

import com.example.eshop.ai.dto.AiIntentResult;
import com.example.eshop.ai.enums.AiIntent;
import com.example.eshop.ai.enums.AiToolRisk;
import com.example.eshop.ai.registry.AiToolDefinition;
import com.example.eshop.ai.service.AiFailure;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiInferenceClientTest {

    private static final String TRACEPARENT = "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01";
    private static final String SECRET_PATH = "/internal/ai/products/SECRET_PATH";

    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void cleanup() {
        if (server != null) {
            server.stop(0);
        }
        RequestContextHolder.resetRequestAttributes();
        MDC.clear();
    }

    @Test
    void sendsToolNamesOnlyAndPropagatesCorrelation() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<com.sun.net.httpserver.Headers> headers = new AtomicReference<>();
        server = listen((exchange) -> {
            headers.set(exchange.getRequestHeaders());
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            reply(exchange, 200, """
                    {"intent":"PRODUCT_GET","tool":"PRODUCT_GET","confidence":0.98,"parameters":{"id":100},"requiresConfirmation":false}
                    """);
        });
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("traceparent", TRACEPARENT);
        request.addHeader("Authorization", "Bearer caller-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MDC.put("requestId", "req_ai_infer");

        AiIntentResult result = client().detect("Show product 100", List.of(productTool()));

        assertThat(result.intent()).isEqualTo(AiIntent.PRODUCT_GET);
        assertThat(result.confidence()).isEqualTo(0.98);
        assertThat(result.parameters().path("id").asInt()).isEqualTo(100);
        assertThat(headers.get().getFirst("X-Request-ID")).isEqualTo("req_ai_infer");
        assertThat(headers.get().getFirst("traceparent")).isEqualTo(TRACEPARENT);
        assertThat(headers.get().getFirst("Authorization")).isNull();
        assertThat(body.get()).contains("PRODUCT_GET").contains("Show product 100").doesNotContain(SECRET_PATH)
                .doesNotContain("catalog-service").doesNotContain("caller-token");
    }

    @Test
    void unknownIntentNamesAreNotExecutable() throws Exception {
        server = listen((exchange) -> {
            exchange.getRequestBody().readAllBytes();
            reply(exchange, 200, """
                    {"intent":"NOTIFICATION_LIST","tool":"NOTIFICATION_LIST","confidence":0.99,"parameters":{"unreadOnly":true}}
                    """);
        });
        AiIntentResult result = client().detect("Show my notifications", List.of());
        assertThat(result.intent()).isEqualTo(AiIntent.UNKNOWN);
        assertThat(result.parameters().isEmpty()).isTrue();
    }

    @Test
    void providerFailuresKeepTheExistingCodes() throws Exception {
        server = listen((exchange) -> {
            exchange.getRequestBody().readAllBytes();
            int status = "timeout".equals(exchange.getRequestHeaders().getFirst("X-Request-ID")) ? 504 : 503;
            String code = status == 504 ? "AI_PROVIDER_TIMEOUT" : "AI_PROVIDER_ERROR";
            reply(exchange, status, "{\"code\":\"" + code + "\",\"message\":\"AI routing is temporarily unavailable\",\"requestId\":\"req\"}");
        });
        MDC.put("requestId", "timeout");
        assertThatThrownBy(() -> client().detect("hello", List.of()))
                .isInstanceOfSatisfying(AiFailure.class, failure -> assertThat(failure.code()).isEqualTo("AI_PROVIDER_TIMEOUT"));
        MDC.put("requestId", "missing-key");
        assertThatThrownBy(() -> client().detect("hello", List.of()))
                .isInstanceOfSatisfying(AiFailure.class, failure -> assertThat(failure.code()).isEqualTo("AI_NOT_CONFIGURED"));
    }

    @Test
    void invalidProviderJsonDoesNotEscape() throws Exception {
        server = listen((exchange) -> {
            exchange.getRequestBody().readAllBytes();
            reply(exchange, 502, "{\"code\":\"AI_INVALID_RESPONSE\",\"message\":\"AI routing is temporarily unavailable\",\"detail\":\"sk-live\"}");
        });
        assertThatThrownBy(() -> client().detect("hello", List.of()))
                .isInstanceOfSatisfying(AiFailure.class, failure -> {
                    assertThat(failure.code()).isEqualTo("AI_INVALID_OUTPUT");
                    assertThat(failure).hasMessage("AI intent detection is unavailable. Please try again later.");
                });
    }

    @Test
    void connectionFailureIsAProviderFailure() {
        var offline = new AiInferenceClient(mapper, "http://127.0.0.1:1", Duration.ofSeconds(2));
        assertThatThrownBy(() -> offline.detect("hello", List.of()))
                .isInstanceOfSatisfying(AiFailure.class, failure -> assertThat(failure.code()).isEqualTo("AI_PROVIDER_FAILURE"));
    }

    private AiInferenceClient client() {
        int port = server.getAddress().getPort();
        return new AiInferenceClient(mapper, "http://127.0.0.1:" + port, Duration.ofSeconds(2));
    }

    private HttpServer listen(com.sun.net.httpserver.HttpHandler handler) throws Exception {
        HttpServer created = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        created.createContext("/api/v1/ai/route", handler);
        created.setExecutor(command -> {
            Thread thread = new Thread(command, "ai-inference-test");
            thread.setDaemon(true);
            thread.start();
        });
        created.start();
        return created;
    }

    private static void reply(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static AiToolDefinition productTool() {
        return new AiToolDefinition(
                AiIntent.PRODUCT_GET,
                "Get an active product",
                "catalog-service",
                "PRODUCT_GET",
                "GET",
                SECRET_PATH,
                Map.of("id", new AiToolDefinition.Parameter("integer", true, "integer")),
                "AUTHENTICATED",
                AiToolRisk.READ_ONLY,
                false,
                true);
    }
}
