package com.example.eshop.ai.service;

import com.example.eshop.ai.model.*;
import com.example.eshop.ai.repository.*;
import com.example.eshop.common.security.CurrentActor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;

/** Durable acceptance happens before provider/tool invocation. Redis is an optional atomic mirror. */
@Service
public class AiQuotaService {

    public record Quota(
            int dailyLimit,
            int used,
            int remaining,
            boolean unlimited,
            OffsetDateTime resetAt
    ) {}

    public static class Exhausted extends RuntimeException {

        public final Quota quota;

        public Exhausted(Quota quota) {
            super("You have reached your daily AI request limit.");
            this.quota = quota;
        }
    }

    private final AiDailyQuotaRepository quotas;
    private final AiQuotaRequestRepository requests;
    private final ObjectMapper mapper;
    private final StringRedisTemplate redis;
    private final TransactionTemplate tx;
    private final int limit;
    private final ZoneId zone;

    private Clock clock = Clock.systemUTC();

    private static final DefaultRedisScript<Long> MIRROR = new DefaultRedisScript<>("""
            local old=tonumber(redis.call('GET',KEYS[1]) or '0')
            local value=math.max(old,tonumber(ARGV[1]))
            redis.call('SET',KEYS[1],value,'EXAT',ARGV[2])
            return value
            """, Long.class);

    public AiQuotaService(
            AiDailyQuotaRepository quotas,
            AiQuotaRequestRepository requests,
            ObjectMapper mapper,
            StringRedisTemplate redis,
            PlatformTransactionManager manager,
            @Value("${ai.quota.daily-limit:5}") int limit,
            @Value("${ai.quota.timezone:Asia/Phnom_Penh}") String timezone
    ) {
        if (limit < 1) {
            throw new IllegalArgumentException("AI daily limit must be positive");
        }

        this.quotas = quotas;
        this.requests = requests;
        this.mapper = mapper;
        this.redis = redis;
        this.limit = limit;
        this.zone = ZoneId.of(timezone);
        tx = new TransactionTemplate(manager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    void clock(Clock value) {
        clock = value;
    }

    private LocalDate day() {
        return LocalDate.now(clock.withZone(zone));
    }

    private boolean unlimited() {
        return CurrentActor.has("ADMIN")
                || CurrentActor.has("SUPER_ADMIN")
                || CurrentActor.has("ROLE_ADMIN")
                || CurrentActor.has("ROLE_SUPER_ADMIN");
    }

    private Quota snapshot(int used, boolean unlimited) {
        return new Quota(
                limit,
                used,
                unlimited ? limit : Math.max(0, limit - used),
                unlimited,
                day().plusDays(1).atStartOfDay(zone).toOffsetDateTime()
        );
    }

    public Quota current() {
        long user = CurrentActor.userId();
        boolean free = unlimited();

        return quotas.findById(user)
                .map(q -> snapshot(
                        q.getBusinessDay().equals(day()) ? q.getUsed() : 0,
                        free
                ))
                .orElseGet(() -> snapshot(0, free));
    }

    public void feedback(UUID key, String rating) {
        if (!Set.of("up", "down").contains(rating)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid feedback"
            );
        }

        String id = CurrentActor.userId() + ":" + key;

        tx.executeWithoutResult(status -> {
            var entry = requests.findById(id)
                    .filter(r -> "COMPLETED".equals(r.getStatus()))
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "AI response not found"
                    ));
            entry.setFeedback(rating);
            requests.saveAndFlush(entry);
        });
    }

    private AiDailyQuota locked(long user) {
        return quotas.lock(user).orElseGet(() -> {
            var q = new AiDailyQuota();
            q.setUserId(user);
            q.setBusinessDay(day());
            return quotas.saveAndFlush(q);
        });
    }

    private String hash(String payload) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest
                            .getInstance("SHA-256")
                            .digest(payload.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public <T> ResponseEntity<T> process(
            UUID key,
            String payload,
            Class<T> type,
            Supplier<ResponseEntity<T>> work
    ) {
        long user = CurrentActor.userId();
        boolean free = unlimited();
        String id = user + ":" + key;
        String fingerprint = hash(payload);
        AiQuotaRequest accepted = null;

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                accepted = tx.execute(status -> {
                    var q = locked(user);
                    var previous = requests.findById(id);

                    if (previous.isPresent()) {
                        var prior = previous.get();

                        if (!fingerprint.equals(prior.getFingerprint())) {
                            throw new ResponseStatusException(
                                    HttpStatus.CONFLICT,
                                    "Idempotency key belongs to another request"
                            );
                        }

                        return prior;
                    }

                    if (!q.getBusinessDay().equals(day())) {
                        q.setBusinessDay(day());
                        q.setUsed(0);
                    }

                    if (!free && q.getUsed() >= limit) {
                        throw new Exhausted(snapshot(q.getUsed(), false));
                    }

                    if (!free) {
                        q.setUsed(q.getUsed() + 1);
                    }

                    quotas.saveAndFlush(q);

                    var entry = new AiQuotaRequest();
                    entry.setId(id);
                    entry.setFingerprint(fingerprint);
                    entry.setStatus("ACCEPTED");
                    entry.setAcceptedAt(clock.instant());
                    entry.setRequestId(
                            com.example.eshop.common.request.RequestIds.current()
                    );
                    entry.setTraceId(org.slf4j.MDC.get("traceId"));
                    return requests.saveAndFlush(entry);
                });
                break;
            } catch (DataIntegrityViolationException race) {
                if (attempt == 2) {
                    throw race;
                }
            }
        }

        if (accepted == null) {
            throw new IllegalStateException("Quota acceptance failed");
        }

        if ("COMPLETED".equals(accepted.getStatus())) {
            try {
                return ResponseEntity
                        .status(accepted.getHttpStatus())
                        .body(mapper.readValue(
                                accepted.getResponseJson(),
                                type
                        ));
            } catch (Exception e) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Stored AI response unavailable"
                );
            }
        }

        if ("FAILED".equals(accepted.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "This accepted request failed; use a new request to try again"
            );
        }

        // Only the transaction that created the record may invoke the provider. Marking it RUNNING is atomic.
        boolean owner = Boolean.TRUE.equals(tx.execute(status -> {
            locked(user);
            var entry = requests.findById(id).orElseThrow();

            if (!"ACCEPTED".equals(entry.getStatus())) {
                return false;
            }

            entry.setStatus("RUNNING");
            requests.saveAndFlush(entry);
            return true;
        }));

        if (!owner) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This AI request is already processing"
            );
        }

        if (!free) {
            var quota = current();

            try {
                redis.execute(
                        MIRROR,
                        List.of("ai:daily:" + user + ":" + day()),
                        String.valueOf(quota.used()),
                        String.valueOf(quota.resetAt().toEpochSecond())
                );
            } catch (RuntimeException ignored) {
                /* PostgreSQL remains authoritative during Redis outages. */
            }
        }

        try {
            ResponseEntity<T> response = work.get();
            String json = mapper.writeValueAsString(response.getBody());

            tx.executeWithoutResult(status -> {
                var entry = requests.findById(id).orElseThrow();
                entry.setStatus("COMPLETED");
                entry.setHttpStatus(response.getStatusCode().value());
                entry.setResponseJson(json);
                requests.saveAndFlush(entry);
            });
            return response;
        } catch (Exception failure) {
            tx.executeWithoutResult(status -> {
                var entry = requests.findById(id).orElseThrow();
                entry.setStatus("FAILED");
                requests.saveAndFlush(entry);
            });

            if (failure instanceof RuntimeException runtime) {
                throw runtime;
            }

            throw new IllegalStateException(failure);
        }
    }
}