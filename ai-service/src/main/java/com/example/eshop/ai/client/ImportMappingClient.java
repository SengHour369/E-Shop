package com.example.eshop.ai.client;

import com.example.eshop.common.imports.ImportTool;
import com.example.eshop.common.request.RequestIds;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;

@Component
public class ImportMappingClient {
    private final RestClient client;
    private final ObjectMapper mapper;
    public ImportMappingClient(@Value("${ai.inference-url:http://localhost:8000}") String url, ObjectMapper mapper) {
        this.mapper = mapper;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }
    public Map<String, String> suggest(ImportTool tool, List<String> headers) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String h : headers) tool.schema.keySet().stream().filter(f -> normalize(f).equals(normalize(h))).findFirst().ifPresent(f -> result.put(h, f));
        try {
            var response = client.post().uri("/api/v1/ai/import-mapping").header(RequestIds.HEADER, RequestIds.current())
                    .body(Map.of("tool", tool.name(), "headers", headers)).retrieve().body(String.class);
            if (response != null && response.length() <= 16384) {
                Map<String, String> proposed = mapper.convertValue(mapper.readTree(response).path("mapping"), new TypeReference<>() {});
                proposed.forEach((h, f) -> { if (headers.contains(h) && tool.schema.containsKey(f) && !result.containsValue(f)) result.put(h, f); });
            }
        } catch (Exception ignored) { /* Exact-header suggestions remain available; confirmation is mandatory. */ }
        return result;
    }
    private static String normalize(String text) { return text.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT); }
    public Map<String, String> explain(Set<String> codes) {
        if (codes.isEmpty()) return Map.of();
        try {
            var response = client.post().uri("/api/v1/ai/import-explain").header(RequestIds.HEADER, RequestIds.current())
                    .body(Map.of("codes", codes.stream().limit(30).toList())).retrieve().body(String.class);
            if (response == null || response.length() > 32768) return Map.of();
            Map<String, String> result = mapper.convertValue(mapper.readTree(response).path("explanations"), new TypeReference<>() {});
            result.entrySet().removeIf(e -> !codes.contains(e.getKey()) || e.getValue() == null || e.getValue().length() > 500);
            return result;
        } catch (Exception ignored) { return Map.of(); }
    }
}
