package com.example.eshop.common.imports;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.security.access.AccessDeniedException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/** Domain writes require a short-lived proof minted only after durable batch approval. */
@Component
public class ImportApprovalProof {
    private final String secret;
    private final ObjectMapper mapper;
    public ImportApprovalProof(@Value("${ai.import.signing-key:}") String secret, ObjectMapper mapper) { this.secret = secret; this.mapper = mapper; }
    public String issue(String key, ImportContracts.Row row, long reviewer, int revision) {
        try {
            String body = Base64.getUrlEncoder().withoutPadding().encodeToString(mapper.writeValueAsBytes(
                    Map.of("key", key, "hash", hash(row), "actor", reviewer, "revision", revision, "expires", Instant.now().plusSeconds(60).getEpochSecond())));
            return body + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(mac(body));
        } catch (Exception ex) { throw new IllegalStateException("Import signing key must be configured with at least 32 characters", ex); }
    }
    public void verify(String proof, String key, ImportContracts.Row row, long actor) {
        try {
            if (proof == null || proof.length() > 2000) throw new IllegalArgumentException();
            String[] parts = proof.split("\\.");
            if (parts.length != 2 || !MessageDigest.isEqual(mac(parts[0]), Base64.getUrlDecoder().decode(parts[1]))) throw new IllegalArgumentException();
            var data = mapper.readTree(Base64.getUrlDecoder().decode(parts[0]));
            if (!data.path("key").asText().equals(key) || !data.path("hash").asText().equals(hash(row))
                    || data.path("actor").asLong() != actor || data.path("revision").asInt() < 1
                    || data.path("expires").asLong() < Instant.now().getEpochSecond()) throw new IllegalArgumentException();
        } catch (Exception ex) { throw new AccessDeniedException("Valid import approval proof required"); }
    }
    private byte[] mac(String body) throws Exception {
        if (secret.length() < 32) throw new IllegalStateException();
        Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
    }
    private String hash(ImportContracts.Row row) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(List.of(row.tool().name(), new TreeMap<>(row.values())))));
    }
}
