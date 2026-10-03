package com.example.eshop.auth.controller;
import com.example.eshop.auth.service.impl.InternalNotificationMailService;
import com.example.eshop.auth.repository.NotificationMailDeliveryRepository;
import com.example.eshop.common.notification.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.dao.DataIntegrityViolationException;
@RestController @RequestMapping("/internal/notifications") @RequiredArgsConstructor
@PreAuthorize("hasAuthority('SERVICE_NOTIFICATION') and authentication.name == 'notification-service'")
public class InternalNotificationController {
    private final InternalNotificationMailService service;
    private final NotificationMailDeliveryRepository deliveries;
    @PostMapping("/email") public void email(@RequestBody NotificationEvent event) {
        event.validate();
        try { service.send(event); }
        catch(DataIntegrityViolationException ex) { if(!deliveries.existsById(event.eventId())) throw ex; }
    }
}
