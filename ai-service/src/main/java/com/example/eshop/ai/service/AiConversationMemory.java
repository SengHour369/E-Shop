package com.example.eshop.ai.service;

import com.example.eshop.ai.registry.AiToolRegistry;
import com.example.eshop.common.security.CurrentActor;
import com.example.eshop.common.security.LivePermissionService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Account and console scoped memory, never authoritative business data or training approval. */
@Service
public class AiConversationMemory {

    private final JdbcTemplate jdbc;
    private final LivePermissionService permissions;

    public AiConversationMemory(
            JdbcTemplate jdbc,
            LivePermissionService permissions
    ) {
        this.jdbc = jdbc;
        this.permissions = permissions;
    }

    private String scope(boolean admin) {
        if (admin && !permissions.current().administrator()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Administrator access required"
            );
        }

        return admin ? "ADMIN" : "CUSTOMER";
    }

    @Transactional
    public void save(
            UUID conversation,
            boolean admin,
            String event,
            String role,
            String channel,
            String content
    ) {
        if (!AiToolRegistry.authenticated()
                || content == null
                || content.isBlank()) {
            return;
        }

        String scope = scope(admin);
        long actor = CurrentActor.userId();

        if (!Set.of("user", "assistant").contains(role)
                || !Set.of("CHAT", "CALL").contains(channel)
                || event.length() > 160) {
            throw new IllegalArgumentException("Invalid memory turn");
        }

        jdbc.update(
                "INSERT INTO ai_memory_conversation(id,actor_id,scope) "
                        + "VALUES (?,?,?) ON CONFLICT(id,actor_id,scope) "
                        + "DO UPDATE SET updated_at=now()",
                conversation,
                actor,
                scope
        );
        jdbc.update(
                "INSERT INTO ai_memory_turn(conversation_id,actor_id,scope,"
                        + "event_id,role,channel,content) VALUES (?,?,?,?,?,?,?) "
                        + "ON CONFLICT(conversation_id,actor_id,scope,event_id,role) "
                        + "DO NOTHING",
                conversation,
                actor,
                scope,
                event,
                role,
                channel,
                content.substring(
                        0,
                        Math.min(4000, content.length())
                )
        );
    }

    @Transactional
    public void exchange(
            UUID conversation,
            boolean admin,
            String question,
            String answer
    ) {
        String event = UUID.randomUUID().toString();
        save(conversation, admin, event, "user", "CHAT", question);
        save(conversation, admin, event, "assistant", "CHAT", answer);
    }

    public boolean owns(UUID id) {
        return AiToolRegistry.authenticated()
                && Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM "
                                + "ai_memory_conversation WHERE id=? "
                                + "AND actor_id=?)",
                        Boolean.class,
                        id,
                        CurrentActor.userId()
                )
        );
    }

    public List<Map<String, String>> recent(UUID id, boolean admin) {
        if (!AiToolRegistry.authenticated()) {
            return List.of();
        }

        var rows = jdbc.query(
                "SELECT role,content FROM ai_memory_turn "
                        + "WHERE conversation_id=? AND actor_id=? AND scope=? "
                        + "ORDER BY id DESC LIMIT 8",
                (r, n) -> Map.of(
                        "role", r.getString("role"),
                        "content", r.getString("content")
                ),
                id,
                CurrentActor.userId(),
                scope(admin)
        );
        Collections.reverse(rows);
        return rows;
    }

    /** Bounded lexical relevance with recency tie-break; short follow-ups use recent account turns. */
    public List<Map<String, String>> relevant(String query, boolean admin) {
        if (!AiToolRegistry.authenticated()
                || query == null
                || query.isBlank()) {
            return List.of();
        }

        String q = query.substring(0, Math.min(1000, query.length()));
        return jdbc.query(
                """
                WITH candidates AS (
                    SELECT id,role,content FROM ai_memory_turn
                    WHERE actor_id=? AND scope=? AND
                      (lower(content) % lower(?) OR lower(?) <% lower(content) OR length(?)<32)
                    ORDER BY id DESC LIMIT 200
                )
                SELECT role,left(content,1000) AS content FROM candidates
                ORDER BY greatest(similarity(lower(content),lower(?)),word_similarity(lower(?),lower(content))) DESC,id DESC LIMIT 4
                """,
                (r, n) -> Map.of(
                        "role", r.getString("role"),
                        "content", r.getString("content")
                ),
                CurrentActor.userId(),
                scope(admin),
                q,
                q,
                q,
                q,
                q
        );
    }

    public List<Map<String, Object>> list(int page) {
        requireAdmin();
        return jdbc.queryForList(
                "SELECT id,actor_id,scope,created_at,updated_at "
                        + "FROM ai_memory_conversation "
                        + "ORDER BY updated_at DESC,id LIMIT 50 OFFSET ?",
                Math.min(Math.max(page, 0), 100000) * 50
        );
    }

    public List<Map<String, Object>> transcript(
            UUID id,
            long actor,
            String scope,
            int page
    ) {
        requireAdmin();
        return jdbc.queryForList(
                "SELECT id,role,channel,content,created_at "
                        + "FROM ai_memory_turn WHERE conversation_id=? "
                        + "AND actor_id=? AND scope=? ORDER BY id "
                        + "LIMIT 100 OFFSET ?",
                id,
                actor,
                scope,
                Math.min(Math.max(page, 0), 100000) * 100
        );
    }

    @Transactional
    public int delete(UUID id, long actor, String scope) {
        requireAdmin();
        return jdbc.update(
                "DELETE FROM ai_memory_conversation "
                        + "WHERE id=? AND actor_id=? AND scope=?",
                id,
                actor,
                scope
        );
    }

    private void requireAdmin() {
        if (!permissions.current().administrator()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Administrator access required"
            );
        }
    }
}