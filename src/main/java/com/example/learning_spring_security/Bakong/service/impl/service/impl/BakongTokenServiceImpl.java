package com.example.learning_spring_security.Bakong.service.impl.service.impl;

import com.example.learning_spring_security.Bakong.service.impl.service.BakongTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BakongTokenServiceImpl implements BakongTokenService {

    private final RestClient restClient;
    private final ObjectMapper mapper;

    @Value("${bakong.base-url}")
    private String baseUrl;
    @Value("${bakong.email}")
    private String email;

    private String cachedToken;
    private Instant tokenExpiry;

    @Override
    public synchronized String getToken() {
        if (cachedToken != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry)) {
            log.info("Using cached token");
            return cachedToken;
        }

        log.info("Renewing token from Bakong");

        try {
            String url = baseUrl.replaceAll("/+$", "") + "/v1/renew_token";

            String responseBody = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Map.of("email", email))
                    .retrieve()
                    .body(String.class);

            JsonNode root = mapper.readTree(responseBody);
            JsonNode tokenNode = root.path("data").path("token");
            if (tokenNode.isMissingNode() || tokenNode.isNull()) {
                throw new RuntimeException("Bakong token not returned");
            }

            cachedToken = tokenNode.asText();

            // Decode JWT to get real expiry
            String[] parts = cachedToken.split("\\.");
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            JsonNode payloadNode = mapper.readTree(payload);
            long exp = payloadNode.path("exp").asLong();
            tokenExpiry = Instant.ofEpochSecond(exp);

            log.info("Obtained new token, expires at {}", tokenExpiry);

            return cachedToken;
        } catch (Exception e) {
            log.error("Failed to obtain Bakong token: {}", e.getMessage());
            throw new RuntimeException("Failed to obtain Bakong token", e);
        }
    }
}