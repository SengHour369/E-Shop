package com.example.eshop.ai.service;

import com.example.eshop.ai.enums.AiExecutionStatus;
import com.example.eshop.common.security.LivePermissionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Only explicitly approved public knowledge is indexed. Live business state uses tools. */
@Service
public class AiKnowledgeService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final LivePermissionService permissions;
    private final RestClient embeddings;
    private final boolean enabled;
    private final String model;
    private final com.example.eshop.common.audit.AuditLogService audit;

    public AiKnowledgeService(JdbcTemplate jdbc, ObjectMapper mapper, LivePermissionService permissions,
            com.example.eshop.common.audit.AuditLogService audit,
            @Value("${ai.knowledge.enabled:false}") boolean enabled,
            @Value("${ai.knowledge.embedding-url:http://localhost:11434}") String url,
            @Value("${ai.knowledge.embedding-model:}") String model) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.permissions = permissions;
        this.audit = audit;
        this.enabled = enabled;
        this.model = model;
        if (!enabled) {
            this.embeddings = null;
            return;
        }
        var http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(8));
        this.embeddings = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }

    public JsonNode retrieve(String query) {
        if (!enabled) {
            return mapper.valueToTree(Map.of("answer", "No approved knowledge is available.", "sources", List.of()));
        }
        try {
            String vector = embed(query);
            var sources = jdbc.query("""
                    select document_id, title, language, content, updated_at
                    from ai_knowledge_chunks
                    where embedding_model = ? and approved = true
                      and 1 - (embedding <=> cast(? as vector)) >= 0.75
                    order by embedding <=> cast(? as vector) limit 3
                    """, (row, index) -> Map.of(
                    "documentId", row.getString("document_id"),
                    "title", row.getString("title"),
                    "language", row.getString("language"),
                    "excerpt", row.getString("content"),
                    "updatedAt", row.getTimestamp("updated_at").toInstant().toString()), model, vector, vector);
            String answer = sources.isEmpty()
                    ? "I could not find reliable approved information for that question."
                    : sources.stream().map(source -> source.get("excerpt")).collect(Collectors.joining("\n\n"));
            return mapper.valueToTree(Map.of("answer", answer, "sources", sources));
        } catch (Exception exception) {
            throw new AiFailure("KNOWLEDGE_UNAVAILABLE", AiExecutionStatus.FAILURE,
                    "Approved knowledge is temporarily unavailable.");
        }
    }

    @Transactional
    public UUID approve(String title, String language, String content) {
        permissions.require("KNOWLEDGE_MANAGE");
        if (!enabled) {
            throw new AiFailure("KNOWLEDGE_DISABLED", AiExecutionStatus.DENIED, "Knowledge indexing is disabled.");
        }
        UUID documentId = UUID.randomUUID();
        index(documentId, title, language, content);
        audit.record(new com.example.eshop.common.audit.AuditEvent(
                com.example.eshop.common.audit.AuditAction.CREATE,
                "AI_KNOWLEDGE", documentId.toString(), null,
                Map.of("language", language, "approved", true),
                com.example.eshop.common.audit.AuditResult.SUCCESS, null));
        return documentId;
    }

    @Transactional
    public UUID replace(UUID documentId, String title, String language, String content) {
        requireEnabled();
        if (jdbc.update("delete from ai_knowledge_chunks where document_id = ?", documentId) == 0) {
            throw new com.example.eshop.common.exception.ResourceNotFoundException("Knowledge document not found");
        }
        // The transaction restores the prior version if embedding or indexing fails.
        index(documentId, title, language, content);
        audit.record(new com.example.eshop.common.audit.AuditEvent(
                com.example.eshop.common.audit.AuditAction.UPDATE,
                "AI_KNOWLEDGE", documentId.toString(), null,
                Map.of("language", language, "approved", true),
                com.example.eshop.common.audit.AuditResult.SUCCESS, null));
        return documentId;
    }

    @Transactional
    public void revoke(UUID documentId) {
        requireEnabled();
        if (jdbc.update("delete from ai_knowledge_chunks where document_id = ?", documentId) == 0) {
            throw new com.example.eshop.common.exception.ResourceNotFoundException("Knowledge document not found");
        }
        audit.record(new com.example.eshop.common.audit.AuditEvent(
                com.example.eshop.common.audit.AuditAction.DELETE,
                "AI_KNOWLEDGE", documentId.toString(), null,
                Map.of("approved", false),
                com.example.eshop.common.audit.AuditResult.SUCCESS, null));
    }

    private void requireEnabled() {
        permissions.require("KNOWLEDGE_MANAGE");
        if (!enabled) {
            throw new AiFailure("KNOWLEDGE_DISABLED", AiExecutionStatus.DENIED, "Knowledge indexing is disabled.");
        }
    }

    private void index(UUID documentId, String title, String language, String content) {
        for (int offset = 0; offset < content.length(); offset += 700) {
            String chunk = content.substring(offset, Math.min(offset + 800, content.length()));
            jdbc.update("""
                    insert into ai_knowledge_chunks
                      (id, document_id, title, language, content, embedding_model, embedding, approved, updated_at)
                    values (?, ?, ?, ?, ?, ?, cast(? as vector), true, current_timestamp)
                    """, UUID.randomUUID(), documentId, title, language, chunk, model, embed(chunk));
            if (offset + 800 >= content.length()) {
                break;
            }
        }
    }

    private String embed(String input) {
        if (model.isBlank()) {
            throw new IllegalStateException("An embedding model must be configured");
        }
        JsonNode response = embeddings.post()
                .uri("/api/embed")
                .body(Map.of("model", model, "input", input))
                .retrieve()
                .body(JsonNode.class);
        JsonNode vector = response == null ? null : response.path("embeddings").path(0);
        if (vector == null || !vector.isArray() || vector.size() < 8 || vector.size() > 4096) {
            throw new IllegalStateException("Invalid embedding");
        }
        for (JsonNode component : vector) {
            if (!component.isNumber() || !Double.isFinite(component.doubleValue())) {
                throw new IllegalStateException("Invalid embedding");
            }
        }
        return vector.toString();
    }
}
