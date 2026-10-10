package com.example.eshop.ai.service;

import com.example.eshop.ai.dto.AiChatResponse;
import com.example.eshop.ai.dto.AiResponse;
import com.example.eshop.common.security.CurrentActor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** Short-lived chat transcripts are scoped to the authenticated actor, never a supplied user ID. */
@Service
@RequiredArgsConstructor
public class AiAdminWorkspaceService {
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private static final Duration TTL = Duration.ofMinutes(30);
    public record Turn(String role, String text, Instant time, AiResponse result, List<String> suggestions) {}
    public record Transcript(UUID id, String title, Instant time, List<Turn> messages) {}
    public record Summary(UUID id, String title, Instant time, String preview) {}
    private String prefix() { return "ai:workspace:" + CurrentActor.userId() + ":"; }
    private String key(UUID id) { return prefix() + id; }
    private String index() { return prefix() + "index"; }
    private Transcript read(UUID id) {
        String json = redis.opsForValue().get(key(id));
        if (json == null) return null;
        try { return mapper.readValue(json, Transcript.class); }
        catch (Exception invalid) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Conversation could not be read"); }
    }
    public Transcript get(UUID id) {
        Transcript transcript = read(id);
        if (transcript == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found or expired");
        return transcript;
    }
    public List<Summary> list() {
        Set<String> ids = redis.opsForZSet().reverseRange(index(), 0, 19);
        if (ids == null) return List.of();
        List<Summary> result = new ArrayList<>();
        for (String value : ids) {
            UUID id = UUID.fromString(value);
            Transcript item = read(id);
            if (item != null) result.add(new Summary(id, item.title(), item.time(), item.messages().isEmpty() ? "" : item.messages().get(0).text()));
        }
        return result;
    }
    private void write(Transcript item) {
        try { redis.opsForValue().set(key(item.id()), mapper.writeValueAsString(item), TTL); }
        catch (com.fasterxml.jackson.core.JsonProcessingException invalid) { throw new IllegalStateException("Conversation could not be saved", invalid); }
        redis.opsForZSet().add(index(), item.id().toString(), item.time().toEpochMilli());
        redis.opsForZSet().removeRange(index(), 0, -21);
        redis.expire(index(), TTL);
    }
    public synchronized void remember(String question, AiChatResponse response) {
        Transcript previous = read(response.conversationId());
        Instant now = Instant.now();
        List<Turn> turns = new ArrayList<>(previous == null ? List.of() : previous.messages());
        turns.add(new Turn("user", question, now, null, List.of()));
        turns.add(new Turn("assistant", response.result().message(), now, response.result(), response.suggestions()));
        if (turns.size() > 40) turns = new ArrayList<>(turns.subList(turns.size() - 40, turns.size()));
        write(new Transcript(response.conversationId(), previous == null ? question.substring(0, Math.min(80, question.length())) : previous.title(), now, turns));
    }
    public synchronized Transcript rename(UUID id, String title) {
        Transcript previous = get(id);
        Transcript renamed = new Transcript(id, title.trim(), previous.time(), previous.messages());
        write(renamed);
        return renamed;
    }
}
