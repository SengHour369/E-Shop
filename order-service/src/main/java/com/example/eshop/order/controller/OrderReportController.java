package com.example.eshop.order.controller;

import com.example.eshop.order.service.OrderReportService;
import com.example.eshop.order.dto.response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders/reports")
@PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
public class OrderReportController extends BaseController {
    private final OrderReportService service;

    @GetMapping("/weekly")
    public ResponseEntity<ResponseErrorTemplate> weekly(
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate weekOf) {
        return ResponseEntity.ok(ResponseErrorTemplate.success("Weekly order report", service.weekly(weekOf)));
    }

    @GetMapping("/monthly")
    public ResponseEntity<ResponseErrorTemplate> monthly(@RequestParam int year) {
        if (year < 2000 || year > 2100) {
            return ResponseEntity.badRequest().body(
                    ResponseErrorTemplate.error("Year must be between 2000 and 2100", "400"));
        }
        return ResponseEntity.ok(ResponseErrorTemplate.success("Monthly order report", service.monthly(year)));
    }
}
