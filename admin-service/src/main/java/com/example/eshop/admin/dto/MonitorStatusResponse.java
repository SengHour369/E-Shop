package com.example.eshop.admin.dto;

import java.time.Instant;

public record MonitorStatusResponse(
        Instant startedAt,
        Instant finishedAt,
        String outcome,
        String detail,
        boolean healthy) {
}
