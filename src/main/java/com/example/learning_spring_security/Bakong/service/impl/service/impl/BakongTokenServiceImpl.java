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
import java.util.Base64;
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

    private static final long SAFETY_SECONDS = 60;

    @Override
    public synchronized String getToken() {

        if (cachedToken != null
                && tokenExpiry != null
                && Instant.now().isBefore(tokenExpiry.minusSeconds(SAFETY_SECONDS))) {
            return cachedToken;
        }

        return refreshToken();
    }
    private String refreshToken() {

        try {
            String url = baseUrl.replaceAll("/+$", "") + "/v1/renew_token";

            String response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Map.of("email", email))
                    .retrieve()
                    .onStatus(status -> status.isError(),
                            (req, res) -> {
                                throw new RuntimeException("Bakong API error: " + res.getStatusCode());
                            })
                    .body(String.class);

            if (response == null || response.isEmpty()) {
                throw new RuntimeException("Empty response from Bakong");
            }

            JsonNode root = mapper.readTree(response);
            JsonNode tokenNode = root.path("data").path("token");

            if (tokenNode.isMissingNode() || tokenNode.isNull()) {
                throw new RuntimeException("Token not returned");
            }

            cachedToken = tokenNode.asText();

            String[] parts = cachedToken.split("\\.");
            if (parts.length != 3) {
                throw new RuntimeException("Invalid JWT format");
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode payloadNode = mapper.readTree(payload);

            long exp = payloadNode.path("exp").asLong();
            tokenExpiry = Instant.ofEpochSecond(exp).minusSeconds(SAFETY_SECONDS);

            log.info("New token generated, expires at {}", tokenExpiry);

            return cachedToken;

        } catch (Exception e) {
            log.error("Failed to refresh Bakong token", e);
            throw new RuntimeException("Token refresh failed", e);
        }
    }
 }

