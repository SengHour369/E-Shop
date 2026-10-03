package com.example.eshop.ai.service;
import com.example.eshop.ai.repository.NotificationOutboxRepository;
import com.example.eshop.common.notification.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
/** A broker acknowledgement may be lost; consumers deduplicate by the stable event ID. */
@Service @RequiredArgsConstructor
public class NotificationOutboxPublisher {
    private final NotificationOutboxRepository repository;
    private final KafkaTemplate<String,String> kafka;
    @Scheduled(fixedDelayString="${notifications.outbox-delay-ms:2000}") @Transactional
    public void publish() {
        for (var row : repository.pending(Instant.now(), PageRequest.of(0, 1))) {
            try {
                kafka.send(NotificationEvent.TOPIC, row.getId().toString(), row.getPayload()).get(5, TimeUnit.SECONDS);
                row.setPublishedAt(Instant.now());
            } catch (InterruptedException ex) { Thread.currentThread().interrupt(); return; }
            catch (Exception ex) {
                row.setAttempts(row.getAttempts() + 1);
                row.setNextAttemptAt(Instant.now().plusSeconds(Math.min(300, 5L * row.getAttempts())));
                org.slf4j.LoggerFactory.getLogger(getClass()).warn("Notification publish deferred eventId={}", row.getId());
            }
        }
    }
}
