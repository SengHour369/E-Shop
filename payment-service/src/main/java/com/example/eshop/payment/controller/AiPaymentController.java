package com.example.eshop.payment.controller;

import com.example.eshop.common.exception.ResourceNotFoundException;
import com.example.eshop.common.security.CurrentActor;
import com.example.eshop.common.security.LivePermissionService;
import com.example.eshop.payment.model.Payment;
import com.example.eshop.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/internal/ai/payments")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiPaymentController {

    private final PaymentRepository payments;
    private final LivePermissionService permissions;

    @GetMapping("/revenue")
    public List<com.example.eshop.payment.dto.response.AiRevenueSummary> revenue(
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(
                    iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        permissions.require("REPORT_VIEW");
        return payments.assistantRevenue(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    @GetMapping("/{id}")
    public PaymentCard mine(@PathVariable long id) {
        permissions.current();
        return payments.findByIdAndUserId(id, CurrentActor.userId())
                .filter(payment -> !Boolean.TRUE.equals(payment.getDeleted()))
                .map(PaymentCard::from)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    }

    @GetMapping("/admin")
    public List<PaymentCard> admin(@RequestParam(required = false) String status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        permissions.require("PAYMENT_VIEW_ALL");
        var page = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("paymentDate"), Sort.Order.desc("id")));
        return payments.findVisibleForAssistant(status, date == null ? null : date.atStartOfDay(),
                        date == null ? null : date.plusDays(1).atStartOfDay(), page)
                .stream()
                .map(PaymentCard::from)
                .toList();
    }

    /** Excludes provider payloads, transaction credentials, QR codes and payment URLs. */
    public record PaymentCard(
            Long id,
            Long orderId,
            String status,
            BigDecimal amount,
            String currency,
            LocalDateTime paymentDate) {

        static PaymentCard from(Payment payment) {
            return new PaymentCard(payment.getId(), payment.getOrderId(), payment.getStatus(),
                    payment.getAmount(), payment.getCurrency(), payment.getPaymentDate());
        }
    }
}
