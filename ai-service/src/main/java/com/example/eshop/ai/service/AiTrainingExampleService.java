package com.example.eshop.ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

/** Approved TRAIN demonstrations only. Never treats examples as live business facts. */
@Service
public class AiTrainingExampleService {

    private final JdbcTemplate jdbc;
    private final boolean enabled;

    public AiTrainingExampleService(
            JdbcTemplate jdbc,
            @Value("${ai.training.retrieval.enabled:false}") boolean enabled
    ) {
        this.jdbc = jdbc;
        this.enabled = enabled;
    }

    public List<Map<String, String>> retrieve(
            String message,
            String language
    ) {
        if (!enabled || message == null || message.isBlank()) {
            return List.of();
        }

        String query = message.strip().substring(
                0,
                Math.min(1000, message.strip().length())
        );

        try {
            return jdbc.query(
                    """
                    SELECT left(prompt,1000) AS prompt,left(response,1500) AS response
                    FROM ai_training.examples
                    WHERE review_status='APPROVED' AND source_verified=true AND consent_confirmed=true
                      AND split='TRAIN' AND language=?
                      AND (lower(prompt) % lower(?) OR lower(?) <% lower(prompt))
                    ORDER BY greatest(similarity(lower(prompt),lower(?)),word_similarity(lower(?),lower(prompt))) DESC,id
                    LIMIT 3
                    """,
                    (row, index) -> Map.of(
                            "prompt", row.getString("prompt"),
                            "response", row.getString("response")
                    ),
                    ("km".equals(language)
                            || query.codePoints().anyMatch(point ->
                            point >= 0x1780 && point <= 0x17ff))
                            ? "km"
                            : "en",
                    query,
                    query,
                    query,
                    query
            );
        } catch (org.springframework.dao.DataAccessException unavailable) {
            // No schema or a temporary DB outage must not make ordinary chat unavailable.
            return List.of();
        }
    }
}