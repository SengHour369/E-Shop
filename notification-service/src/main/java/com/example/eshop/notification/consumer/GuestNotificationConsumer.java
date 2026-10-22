package com.example.eshop.notification.consumer;
import com.example.eshop.common.notification.GuestNotificationEvent;
import com.example.eshop.common.automation.ServiceIdentity;
import com.example.eshop.common.jwt.JwtProperties;
import com.example.eshop.notification.client.AuthMailClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component @RequiredArgsConstructor
public class GuestNotificationConsumer {
    private final ObjectMapper json;
    private final AuthMailClient mail;
    private final JwtProperties jwt;
    @KafkaListener(topics=GuestNotificationEvent.TOPIC,groupId="notification-service-guest-v1")
    public void receive(String payload) {
        if(payload==null||payload.length()>8192) throw new IllegalArgumentException("Invalid event size");
        GuestNotificationEvent event;
        try {event=json.readValue(payload,GuestNotificationEvent.class);event.validate();}
        catch(Exception e) {throw new IllegalArgumentException("Invalid guest notice schema");}
        // Auth mail relay resolves a verified recipient from the authoritative order; Kafka supplies no email address.
        mail.sendGuest(ServiceIdentity.bearer(jwt,"notification-service","SERVICE_NOTIFICATION"),event);
    }
}
