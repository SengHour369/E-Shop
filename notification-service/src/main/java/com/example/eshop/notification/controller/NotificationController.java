package com.example.eshop.notification.controller;
import com.example.eshop.notification.service.NotificationService;
import com.example.eshop.notification.model.Notification;
import com.example.eshop.common.notification.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.Instant;
import java.util.Map;
@RestController @RequestMapping("/api/notifications") @RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;
    @GetMapping public Object list(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size,
        @RequestParam(required=false) Notification.Status status, @RequestParam(required=false) NotificationEvent.Type type,
        @RequestParam(required=false) Boolean read,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant to) { return service.list(page,size,status,type,read,from,to); }
    @GetMapping("/{id}") public Object get(@PathVariable long id) { return service.get(id); }
    @GetMapping("/unread-count") public Object count() { return Map.of("count",service.unread()); }
    @PatchMapping("/{id}/read") public void read(@PathVariable long id) { service.markRead(id); }
    @PatchMapping("/read-all") public Object readAll() { return Map.of("updated",service.markAll()); }
    public record Preferences(boolean aiEmail) {}
    @GetMapping("/preferences") public Object preferences() { return new Preferences(service.emailPreference()); }
    @PatchMapping("/preferences") public void preferences(@RequestBody Preferences p) { service.preference(p.aiEmail()); }
}
