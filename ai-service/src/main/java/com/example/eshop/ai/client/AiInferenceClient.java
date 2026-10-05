package com.example.eshop.ai.client;

import com.example.eshop.ai.dto.AiIntentResult;
import com.example.eshop.ai.enums.AiExecutionStatus;
import com.example.eshop.ai.enums.AiIntent;
import com.example.eshop.ai.registry.AiToolDefinition;
import com.example.eshop.ai.service.AiFailure;
import com.example.eshop.common.request.RequestIds;
import com.example.eshop.common.request.TraceContextFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Asks ai-inference for one intent. Tool URLs, the caller bearer token, and the provider key
 * are not sent. Spring Boot still authorizes and executes the result.
 */
@Component
public class AiInferenceClient implements AiProviderClient {

    private static final Logger log = LoggerFactory.getLogger(AiInferenceClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final int MAX_RESPONSE_CHARS = 65536;
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
    private static final String UNAVAILABLE = "AI intent detection is unavailable. Please try again later.";

    private final ObjectMapper mapper;
    private final URI endpoint;
    private final Duration requestTimeout;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Autowired
    public AiInferenceClient(ObjectMapper mapper,
            @Value("${ai.inference-url:http://localhost:8000}") String baseUrl,
            @Value("${ai.inference-timeout-seconds:25}") long timeoutSeconds) {
        this(mapper, baseUrl, Duration.ofSeconds(Math.max(1, timeoutSeconds)));
    }

    AiInferenceClient(ObjectMapper mapper, String baseUrl, Duration requestTimeout) {
        this.mapper = mapper;
        this.endpoint = endpoint(baseUrl);
        this.requestTimeout = requestTimeout;
    }

    @Override
    public AiIntentResult detect(String message, List<AiToolDefinition> allowed) {
        if (message == null || message.isBlank()) {
            throw failure("AI_PROVIDER_FAILURE");
        }
        try {
            HttpResponse<String> response = http.send(request(message, allowed), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw failure(codeFor(response.statusCode(), response.body()));
            }
            AiIntentResult result = toResult(response.body());
            log.info("AI inference completed intent={} status={}", result.intent(), response.statusCode());
            return result;
        } catch (HttpTimeoutException ex) {
            throw failure("AI_PROVIDER_TIMEOUT");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw failure("AI_INTERRUPTED");
        } catch (AiFailure ex) {
            throw ex;
        } catch (Exception ex) {
            throw failure("AI_PROVIDER_FAILURE");
        }
    }

    private HttpRequest request(String message, List<AiToolDefinition> allowed) throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "message", message,
                "tools", toolPayloads(allowed)));
        HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        String requestId = RequestIds.current();
        if (requestId != null && SAFE_ID.matcher(requestId).matches()) {
            builder.header(RequestIds.HEADER, requestId);
        }
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servlet) {
            String parent = servlet.getRequest().getHeader("traceparent");
            if (TraceContextFilter.valid(parent)) {
                builder.header("traceparent", parent);
            }
        }
        return builder.build();
    }

    /** Names, descriptions, and parameter types only. Paths and permissions stay in the registry. */
    private static List<Map<String, Object>> toolPayloads(List<AiToolDefinition> allowed) {
        List<Map<String, Object>> tools = new ArrayList<>();
        if (allowed == null) {
            return tools;
        }
        for (AiToolDefinition tool : allowed) {
            Map<String, String> parameters = new TreeMap<>();
            tool.requestSchema().forEach((name, spec) -> parameters.put(name, spec.type()));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", tool.toolName().name());
            item.put("description", tool.description());
            item.put("parameters", parameters);
            tools.add(item);
        }
        return tools;
    }

    private AiIntentResult toResult(String body) throws Exception {
        if (body == null || body.length() > MAX_RESPONSE_CHARS) {
            throw failure("AI_PROVIDER_FAILURE");
        }
        JsonNode root = mapper.readTree(body);
        if (!root.path("intent").isTextual() || !root.path("confidence").isNumber()) {
            throw failure("AI_INVALID_OUTPUT");
        }
        double confidence = root.path("confidence").asDouble();
        if (!Double.isFinite(confidence)) {
            throw failure("AI_INVALID_OUTPUT");
        }
        JsonNode parameters = root.path("parameters");
        if (!parameters.isMissingNode() && !parameters.isObject()) {
            throw failure("AI_INVALID_OUTPUT");
        }
        if (!parameters.isObject()) {
            parameters = mapper.createObjectNode();
        }
        AiIntent intent = knownIntent(root.path("intent").asText());
        if (intent == AiIntent.UNKNOWN) {
            parameters = mapper.createObjectNode();
        }
        return new AiIntentResult(intent, confidence, parameters);
    }

    private static AiIntent knownIntent(String name) {
        try {
            return AiIntent.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return AiIntent.UNKNOWN;
        }
    }

    private String codeFor(int status, String body) {
        String reported = "";
        try {
            if (body != null && body.length() <= MAX_RESPONSE_CHARS && !body.isBlank()) {
                JsonNode root = mapper.readTree(body);
                reported = root.path("code").asText("");
            }
        } catch (Exception ex) {
            reported = "";
        }
        if (status == 504 || "AI_PROVIDER_TIMEOUT".equals(reported)) {
            return "AI_PROVIDER_TIMEOUT";
        }
        if (status == 503) {
            return "AI_NOT_CONFIGURED";
        }
        if ("AI_INVALID_RESPONSE".equals(reported)) {
            return "AI_INVALID_OUTPUT";
        }
        return "AI_PROVIDER_FAILURE";
    }

    private static URI endpoint(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("ai.inference-url is required");
        }
        String origin = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        URI uri = URI.create(origin);
        String scheme = uri.getScheme();
        if (uri.getUserInfo() != null
                || uri.getHost() == null
                || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("ai.inference-url must be an http(s) URL without credentials");
        }
        return URI.create(origin + "/api/v1/ai/route");
    }

    private static AiFailure failure(String code) {
        log.warn("AI inference failed code={}", code);
        return new AiFailure(code, AiExecutionStatus.FAILURE, UNAVAILABLE);
    }
}
