package com.example.eshop.auth.service.impl;
import com.example.eshop.auth.model.NotificationMailDelivery;
import com.example.eshop.auth.repository.*;
import com.example.eshop.common.notification.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
@Service @RequiredArgsConstructor
public class InternalNotificationMailService {
    private final NotificationMailDeliveryRepository deliveries;
    private final UserRepository users;
    private final EmailNotificationService email;
    @Transactional
    public void send(NotificationEvent e) {
        e.validate();
        if(deliveries.existsById(e.eventId())) return;
        var user=users.findById(e.userId()).filter(u -> u.isEnabled() && !Boolean.TRUE.equals(u.getDeleted()))
            .orElseThrow(() -> new IllegalArgumentException("Notification recipient unavailable"));
        var delivery=new NotificationMailDelivery(); delivery.setEventId(e.eventId()); delivery.setUserId(e.userId()); delivery.setSentAt(Instant.now());
        // Unique insertion serializes concurrent redelivery before SMTP. SMTP still has an unavoidable acknowledgement crash window.
        deliveries.saveAndFlush(delivery);
        boolean success=e.type()==NotificationEvent.Type.AI_ACTION_COMPLETED;
        email.sendSimpleEmail(user.getEmail(),success ? "E-Shop: AI action completed" : "E-Shop: AI action needs attention",
            (success ? "Your requested action completed." : "Your requested action could not be confirmed. Check its execution before trying again.")
            +"\nExecution: "+e.aiExecutionId()+"\nRequest: "+e.requestId());
    }
}
