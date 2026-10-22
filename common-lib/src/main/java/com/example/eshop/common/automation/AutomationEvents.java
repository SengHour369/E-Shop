package com.example.eshop.common.automation;

import com.example.eshop.common.notification.NotificationEvent;
import com.example.eshop.common.request.RequestIds;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

/** Joins the caller's local transaction: business change and outbox cannot commit separately. */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "automation.enabled", havingValue = "true")
public class AutomationEvents {

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    @org.springframework.beans.factory.annotation.Value("${automation.admin-user-ids:}")
    private java.util.List<Long> adminRecipients;

    @Transactional(propagation = Propagation.MANDATORY)
    public void emit(String topic, CommerceEvent event) {
        event.validate();
        insert(
                event.eventId(),
                "OUTBOX",
                topic,
                event.orderId(),
                event.type().name(),
                event
        );
    }

    @Transactional
    public void receive(String topic, CommerceEvent event) {
        event.validate();
        insert(
                event.eventId(),
                "INBOX",
                topic,
                event.orderId(),
                event.type().name(),
                event
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void notify(CommerceEvent event, NotificationEvent.Type type) {
        java.util.Set<Long> recipients = new java.util.LinkedHashSet<>();
        if (event.userId() != null) recipients.add(event.userId());
        else {
            UUID id=UUID.nameUUIDFromBytes((event.eventId()+":guest:"+type).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var guest=new com.example.eshop.common.notification.GuestNotificationEvent(id,event.orderId(),type,event.requestId(),event.traceId(),Instant.now());
            guest.validate();insert(id,"OUTBOX",com.example.eshop.common.notification.GuestNotificationEvent.TOPIC,event.orderId(),type.name(),guest);
        }

        if (adminRecipients != null) {
            recipients.addAll(adminRecipients);
        }

        for (long recipient : recipients) {
            if (recipient <= 0) {
                throw new IllegalStateException(
                        "Invalid configured notification recipient"
                );
            }

            UUID id = UUID.nameUUIDFromBytes(
                    (event.eventId() + ":" + type + ":" + recipient)
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
            NotificationEvent notification = new NotificationEvent(
                    1,
                    id,
                    event.requestId(),
                    event.traceId(),
                    null,
                    null,
                    recipient,
                    type,
                    "ORDER",
                    Long.toString(event.orderId()),
                    Instant.now()
            );
            notification.validate();
            insert(
                    id,
                    "OUTBOX",
                    NotificationEvent.TOPIC,
                    event.orderId(),
                    type.name(),
                    notification
            );
        }
    }

    private void insert(
            UUID id,
            String direction,
            String topic,
            long orderId,
            String type,
            Object event
    ) {
        try {
            String payload = json.writeValueAsString(event);
            String requestId = event instanceof CommerceEvent e
                    ? e.requestId()
                    : event instanceof com.example.eshop.common.notification.GuestNotificationEvent g ? g.requestId() : ((NotificationEvent) event).requestId();
            String traceId = event instanceof CommerceEvent e
                    ? e.traceId()
                    : event instanceof com.example.eshop.common.notification.GuestNotificationEvent g ? g.traceId() : ((NotificationEvent) event).traceId();
            jdbc.update(
                    "INSERT INTO automation_events(event_id,direction,topic,"
                            + "order_id,event_type,payload,request_id,trace_id) "
                            + "VALUES(?,?,?,?,?,?,?,?) "
                            + "ON CONFLICT(event_id,direction) DO NOTHING",
                    id,
                    direction,
                    topic,
                    orderId,
                    type,
                    payload,
                    requestId,
                    traceId
            );
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Event serialization failed",
                    ex
            );
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void review(long orderId, String code, String summary) {
        jdbc.update(
                "INSERT INTO automation_reviews(order_id,code,summary,request_id) "
                        + "VALUES(?,?,?,?) "
                        + "ON CONFLICT(order_id,code) WHERE state='OPEN' DO NOTHING",
                orderId,
                code,
                summary,
                RequestIds.resolve(RequestIds.current())
        );
    }

    public static CommerceEvent event(CommerceEvent.Type type,long id,int owner,java.math.BigDecimal amount,String method,Long paymentId) {
        return event(type,id,Long.valueOf(owner),amount,method,paymentId);
    }

    public static CommerceEvent event(
            CommerceEvent.Type type,
            long id,
            Long owner,
            java.math.BigDecimal amount,
            String method,
            Long paymentId
    ) {
        return new CommerceEvent(
                owner == null ? 2 : 1,
                UUID.randomUUID(),
                type,
                id,
                owner,
                amount,
                "USD",
                method,
                paymentId,
                RequestIds.resolve(RequestIds.current()),
                MDC.get("traceId"),
                Instant.now()
        );
    }
}