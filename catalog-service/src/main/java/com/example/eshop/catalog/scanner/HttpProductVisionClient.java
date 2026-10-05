package com.example.eshop.catalog.scanner;

import com.example.eshop.common.request.RequestIds;
import com.example.eshop.common.request.TraceContextFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Sends one still image to ai-inference. The model does not receive tokens, URLs, or catalog ids. */
public class HttpProductVisionClient implements ProductVisionClient {
    private static final Logger log = LoggerFactory.getLogger(HttpProductVisionClient.class);
    private static final int MAX_RESPONSE_CHARS = 65536;

    private final ObjectMapper mapper;
    private final URI endpoint;
    private final Duration timeout;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public HttpProductVisionClient(ObjectMapper mapper, String baseUrl, Duration timeout) {
        this.mapper = mapper;
        this.timeout = timeout;
        this.endpoint = endpoint(baseUrl);
    }

    @Override
    public VisionOutcome identify(byte[] image, String mediaType) {
        if (endpoint == null || image == null || image.length == 0) {
            return VisionOutcome.unavailable();
        }
        try {
            String body = mapper.writeValueAsString(java.util.Map.of(
                    "mediaType", mediaType,
                    "imageBase64", Base64.getEncoder().encodeToString(image)));
            HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            String requestId = RequestIds.current();
            if (requestId != null) {
                builder.header(RequestIds.HEADER, requestId);
            }
            String traceparent = traceparent();
            if (traceparent != null) {
                builder.header("traceparent", traceparent);
            }
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body() == null || response.body().length() > MAX_RESPONSE_CHARS) {
                log.info("Product vision unavailable status={}", response.statusCode());
                return VisionOutcome.unavailable();
            }
            return new VisionOutcome(true, labels(mapper.readTree(response.body())));
        } catch (HttpTimeoutException ex) {
            log.info("Product vision timed out");
            return VisionOutcome.unavailable();
        } catch (Exception ex) {
            log.info("Product vision unavailable");
            return VisionOutcome.unavailable();
        }
    }

    private static List<VisionLabel> labels(JsonNode root) {
        JsonNode rows = root == null ? null : root.get("candidates");
        if (rows == null || !rows.isArray()) {
            return List.of();
        }
        List<VisionLabel> labels = new ArrayList<>();
        for (JsonNode row : rows) {
            if (labels.size() == 5 || row == null || !row.hasNonNull("name")) {
                continue;
            }
            String name = row.get("name").asText("").trim();
            if (name.isEmpty() || name.length() > 160) {
                continue;
            }
            double confidence = row.path("confidence").asDouble(Double.NaN);
            if (Double.isNaN(confidence)) {
                continue;
            }
            labels.add(new VisionLabel(name, confidence));
        }
        return List.copyOf(labels);
    }

    private static String traceparent() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servlet)) {
            return null;
        }
        String parent = servlet.getRequest().getHeader("traceparent");
        return TraceContextFilter.valid(parent) ? parent : null;
    }

    private static URI endpoint(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        String root = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return URI.create(root + "/api/v1/ai/vision/products");
    }
}
