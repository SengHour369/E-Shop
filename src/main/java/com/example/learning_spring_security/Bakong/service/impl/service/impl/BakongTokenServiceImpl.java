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
    private Instant actualTokenExpiry;

    private static final long SAFETY_SECONDS = 60;

    @Override
    public synchronized String getToken() {
        Instant now = Instant.now();

        // Token still fresh inside safety window — return immediately
        if (cachedToken != null && tokenExpiry != null && now.isBefore(tokenExpiry)) {
            return cachedToken;
        }

        // Try to refresh
        try {
            return refreshToken();
        } catch (Exception e) {
            // Refresh failed — fall back to stale token if it has not actually expired yet
            if (cachedToken != null && actualTokenExpiry != null && now.isBefore(actualTokenExpiry)) {
                log.warn("Bakong token refresh failed. Falling back to stale token (expires at {}): {}",
                        actualTokenExpiry, e.getMessage());
                return cachedToken;
            }
            // Token is truly expired and refresh failed — clear state so next call retries
            log.error("Bakong token refresh failed and no valid fallback token available. " +
                    "Payments requiring token verification will fail until Bakong is reachable.", e);
            cachedToken = null;
            tokenExpiry = null;
            actualTokenExpiry = null;
            throw new RuntimeException("Bakong token unavailable: " + e.getMessage(), e);
        }
    }

    private String refreshToken() throws Exception {
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
            throw new RuntimeException("Empty response from Bakong token endpoint");
        }

        JsonNode root = mapper.readTree(response);
        JsonNode tokenNode = root.path("data").path("token");

        if (tokenNode.isMissingNode() || tokenNode.isNull()) {
            throw new RuntimeException("Token field missing in Bakong response");
        }

        String newToken = tokenNode.asText();

        String[] parts = newToken.split("\\.");
        if (parts.length != 3) {
            throw new RuntimeException("Bakong returned an invalid JWT format");
        }

        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        JsonNode payloadNode = mapper.readTree(payload);
        long exp = payloadNode.path("exp").asLong();

        cachedToken = newToken;
        actualTokenExpiry = Instant.ofEpochSecond(exp);
        tokenExpiry = actualTokenExpiry.minusSeconds(SAFETY_SECONDS);

        log.info("Bakong token refreshed successfully, expires at {} (safety cutoff: {})",
                actualTokenExpiry, tokenExpiry);
        return cachedToken;
    }
}
