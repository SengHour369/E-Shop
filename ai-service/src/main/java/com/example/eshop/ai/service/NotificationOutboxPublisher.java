package com.example.eshop.ai.service;

import com.example.eshop.ai.model.NotificationOutbox;
import com.example.eshop.ai.repository.NotificationOutboxRepository;
import com.example.eshop.common.notification.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/** Publishes one pending outbox row per tick. Consumers deduplicate by the stable event id. */
@Service
@RequiredArgsConstructor
public class NotificationOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxPublisher.class);
    private static final int BATCH_SIZE = 1;
    private static final int ACK_TIMEOUT_SECONDS = 5;
    private static final long MAX_BACKOFF_SECONDS = 300;
    private static final long BACKOFF_STEP_SECONDS = 5;

    private final NotificationOutboxRepository repository;
    private final KafkaTemplate<String, String> kafka;

    @Scheduled(fixedDelayString = "${notifications.outbox-delay-ms:2000}")
    @Transactional
    public void publish() {
        var pending = repository.pending(Instant.now(), PageRequest.of(0, BATCH_SIZE));
        for (NotificationOutbox row : pending) {
            try {
                kafka.send(NotificationEvent.TOPIC, row.getId().toString(), row.getPayload())
                        .get(ACK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                row.setPublishedAt(Instant.now());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception ex) {
                row.setAttempts(row.getAttempts() + 1);
                row.setNextAttemptAt(Instant.now().plusSeconds(backoffSeconds(row.getAttempts())));
                log.warn("Notification publish deferred eventId={}", row.getId());
            }
        }
    }

    private static long backoffSeconds(int attempts) {
        return Math.min(MAX_BACKOFF_SECONDS, BACKOFF_STEP_SECONDS * attempts);
    }
}
