package com.example.eshop.ai;
import com.example.eshop.ai.model.NotificationOutbox;
import com.example.eshop.ai.repository.NotificationOutboxRepository;
import com.example.eshop.ai.service.NotificationOutboxPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import java.util.*;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;
class OutboxPublisherTest {
    @Test void brokerFailureLeavesEventPendingWithStableId() {
        var repo=mock(NotificationOutboxRepository.class);
        KafkaTemplate<String,String> kafka=mock(KafkaTemplate.class);
        var row=new NotificationOutbox(); row.setId(UUID.randomUUID()); row.setPayload("{}");
        when(repo.pending(any(),any())).thenReturn(List.of(row));
        when(kafka.send(anyString(),anyString(),anyString())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException()));
        var service=new NotificationOutboxPublisher(repo,kafka); service.publish();
        assertThat(row.getPublishedAt()).isNull(); assertThat(row.getNextAttemptAt()).isAfter(Instant.now());
        when(kafka.send(anyString(),anyString(),anyString())).thenReturn(CompletableFuture.completedFuture(null));
        service.publish(); assertThat(row.getPublishedAt()).isNotNull();
        verify(kafka,times(2)).send(anyString(),eq(row.getId().toString()),eq("{}"));
    }
}
