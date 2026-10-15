package com.example.eshop.common.automation;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "automation.enabled", havingValue = "true")
public class AutomationConfiguration {

    @Bean
    NewTopic commerceNotifications() {
        return topic(
                com.example.eshop.common.notification.NotificationEvent.TOPIC
        );
    }

    @Bean
    NewTopic commerceNotificationsDead() {
        return topic(
                com.example.eshop.common.notification.NotificationEvent.TOPIC
                        + ".DLT"
        );
    }

    @Bean
    NewTopic commerceOrders() {
        return topic(CommerceEvent.ORDERS);
    }

    @Bean
    NewTopic commercePayments() {
        return topic(CommerceEvent.PAYMENTS);
    }

    @Bean
    NewTopic commerceOrdersDead() {
        return topic(CommerceEvent.ORDERS + ".DLT");
    }

    @Bean
    NewTopic commercePaymentsDead() {
        return topic(CommerceEvent.PAYMENTS + ".DLT");
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    DefaultErrorHandler commerceErrorHandler(
            KafkaTemplate<String, String> kafka
    ) {
        var recoverer = new DeadLetterPublishingRecoverer(kafka);
        recoverer.setFailIfSendResultIsError(true);
        var handler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(1000, 3)
        );
        handler.setAckAfterHandle(true);
        return handler;
    }
}