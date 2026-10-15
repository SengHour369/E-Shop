package com.example.eshop.notification.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class NotificationAutomationController {

    private final JdbcTemplate jdbc;

    @GetMapping("/api/notifications/admin/delivery")
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<Map<String, Object>> list(
            @RequestParam(required = false) Long orderId
    ) {
        return records(orderId);
    }

    @GetMapping("/internal/commerce/notifications/{orderId}")
    @PreAuthorize("hasAuthority('SERVICE_ORDER')")
    public List<Map<String, Object>> order(@PathVariable long orderId) {
        return records(orderId);
    }

    private List<Map<String, Object>> records(Long orderId) {
        return jdbc.queryForList(
                "SELECT id,user_id,type,resource_type,resource_id,status,"
                        + "email_status,email_attempts,created_at,sent_at,"
                        + "emailed_at,request_id,trace_id FROM notifications "
                        + "WHERE (CAST(? AS VARCHAR) IS NULL OR "
                        + "(resource_type='ORDER' AND resource_id=?)) "
                        + "ORDER BY created_at DESC LIMIT 100",
                orderId == null ? null : Long.toString(orderId),
                orderId == null ? null : Long.toString(orderId)
        );
    }
}