package com.example.eshop.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Local conversational model has no access to service credentials or business results. */
@Service
public class AiConversationService {
    private final ObjectMapper mapper;
    private final URI endpoint;
    private final Duration timeout;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    public AiConversationService(ObjectMapper mapper,
            @Value("${ai.inference-url:http://localhost:8000}") String base,
            @Value("${ai.inference-timeout-seconds:25}") long seconds) {
        this.mapper = mapper;
        endpoint = URI.create(base.replaceAll("/+$", "") + "/api/v1/ai/conversation");
        timeout = Duration.ofSeconds(seconds);
    }
    public String reply(String message, String language, List<Map<String, String>> history) {
        try {
            var body = mapper.writeValueAsString(Map.of("message", message, "language", language == null ? "en" : language, "history", history == null ? List.of() : history));
            var request = HttpRequest.newBuilder(endpoint).timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body().length() > 65536) return null;
            String answer = mapper.readTree(response.body()).path("message").asText();
            return answer.isBlank() ? null : answer.substring(0, Math.min(4000, answer.length()));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception unavailable) {
            return null;
        }
    }
}

