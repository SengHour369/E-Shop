package com.example.learning_spring_security.Bakong.service.impl.controller;

import com.example.learning_spring_security.Model.OrderDetail;
import com.example.learning_spring_security.Repository.OrderRepository;
import com.example.learning_spring_security.Repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/bakong")
@RequiredArgsConstructor
@Slf4j
public class BakongWebhookController {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @PostMapping("/payment")
    public ResponseEntity<String> handlePaymentCallback(@RequestBody Map<String, Object> payload) {
        log.info("Received Bakong webhook: {}", payload);

        // Parse the fields according to Bakong's actual webhook payload structure.
        // These field names are examples – adjust to match Bakong's documentation.
        String transactionId = (String) payload.get("transactionId");
        String md5 = (String) payload.get("md5");
        String billNumber = (String) payload.get("billNumber"); // your order reference
        String amount = (String) payload.get("amount");

        // Find the order by your reference (e.g., order number or bill number)
        OrderDetail order = orderRepository.findByOrderNumber(billNumber)
                .orElseThrow(() -> new RuntimeException("Order not found for billNumber: " + billNumber));

        // Store the Bakong transaction ID (prefer the MD5 if available, otherwise the transactionId)
        String bakongTxId = md5 != null ? md5 : transactionId;
        if (bakongTxId == null) {
            return ResponseEntity.badRequest().body("Missing transaction identifier");
        }

        order.getPayment().setTransactionId(bakongTxId);
        order.getPayment().setStatus("PENDING"); // or PROCESSING – update later after verification
        paymentRepository.save(order.getPayment());

        return ResponseEntity.ok("OK");
    }
}