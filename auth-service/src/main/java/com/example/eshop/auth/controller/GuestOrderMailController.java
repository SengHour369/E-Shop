package com.example.eshop.auth.controller;
import com.example.eshop.auth.service.impl.GuestOrderMailService;
import com.example.eshop.auth.repository.GuestMailDeliveryRepository;
import com.example.eshop.common.notification.GuestNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.dao.DataIntegrityViolationException;

@RestController @RequiredArgsConstructor
public class GuestOrderMailController {
    private final GuestOrderMailService mail;
    private final GuestMailDeliveryRepository deliveries;
    @PostMapping("/internal/notifications/guest-email")
    @PreAuthorize("hasAuthority('SERVICE_NOTIFICATION') and authentication.name == 'notification-service'")
    public void send(@RequestBody GuestNotificationEvent event) {
        event.validate();try {mail.send(event);} catch(DataIntegrityViolationException e) {if(!deliveries.existsById(event.eventId())) throw e;}
    }
}
