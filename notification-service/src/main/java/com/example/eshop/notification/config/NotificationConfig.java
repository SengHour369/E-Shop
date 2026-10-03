package com.example.eshop.notification.config;
import com.example.eshop.common.notification.NotificationEvent;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.*;
import org.springframework.util.backoff.FixedBackOff;
import org.apache.kafka.clients.admin.NewTopic;
@Configuration
public class NotificationConfig {
    @Bean NewTopic notificationsTopic() { return TopicBuilder.name(NotificationEvent.TOPIC).partitions(3).replicas(1).build(); }
    @Bean NewTopic notificationsDeadLetterTopic() { return TopicBuilder.name(NotificationEvent.TOPIC+".DLT").partitions(3).replicas(1).build(); }
    @Bean DefaultErrorHandler notificationErrorHandler(KafkaTemplate<String,String> template) {
        var recoverer = new DeadLetterPublishingRecoverer(template);
        recoverer.setFailIfSendResultIsError(true);
        var handler = new DefaultErrorHandler(recoverer,new FixedBackOff(1000,3));
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;
    }
}
