package com.example.eshop.notification.consumer;
import com.example.eshop.common.notification.NotificationEvent;
import com.example.eshop.notification.service.NotificationService;
import com.example.eshop.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.MDC;
@Component @RequiredArgsConstructor
public class NotificationEventConsumer {
    private final ObjectMapper mapper;
    private final NotificationService service;
    private final NotificationRepository repository;
    @KafkaListener(topics=NotificationEvent.TOPIC, groupId="notification-service-v1")
    public void consume(String payload) {
        if (payload == null || payload.length() > 8192) throw new IllegalArgumentException("Invalid event size");
        NotificationEvent e;
        try { e = mapper.readValue(payload, NotificationEvent.class); e.validate(); }
        catch(Exception ex) { throw new IllegalArgumentException("Invalid event schema"); }
        var previous = MDC.getCopyOfContextMap();
        try {
            MDC.clear(); MDC.put("requestId",e.requestId()); MDC.put("eventId",e.eventId().toString());
            MDC.put("aiExecutionId",e.aiExecutionId().toString());
            if(e.traceId()!=null) MDC.put("traceId",e.traceId());
            if(e.spanId()!=null) MDC.put("parentSpanId",e.spanId());
            try { service.receive(e); }
            catch(DataIntegrityViolationException ex) { if(!repository.existsByEventId(e.eventId())) throw ex; }
        } finally { MDC.clear(); if(previous!=null) MDC.setContextMap(previous); }
    }
}
