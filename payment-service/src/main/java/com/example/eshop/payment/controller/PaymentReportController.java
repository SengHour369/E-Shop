package com.example.eshop.payment.controller;

import com.example.eshop.payment.service.PaymentReportService;
import com.example.eshop.payment.dto.response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments/reports")
@PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
public class PaymentReportController extends BaseController {
    private final PaymentReportService service;

    @GetMapping("/monthly")
    public ResponseEntity<ResponseErrorTemplate> monthly(@RequestParam int year) {
        if (year < 2000 || year > 2100) {
            return ResponseEntity.badRequest().body(
                    ResponseErrorTemplate.error("Year must be between 2000 and 2100", "400"));
        }
        return ResponseEntity.ok(ResponseErrorTemplate.success("Monthly payment report", service.monthly(year)));
    }
}
