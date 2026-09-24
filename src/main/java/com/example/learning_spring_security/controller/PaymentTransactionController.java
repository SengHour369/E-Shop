package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Service.ServiceStructure.PaymentTransactionService;
import com.example.learning_spring_security.dto.Request.GetPaymentTransactionRequest;
import com.example.learning_spring_security.dto.Request.PaymentTransactionRequest;
import com.example.learning_spring_security.dto.Request.PaymentTransactionStatusUpdateRequest;
import com.example.learning_spring_security.dto.Response.PaymentTransactionResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payment-transactions")
@RequiredArgsConstructor
public class PaymentTransactionController extends BaseController {

    private final PaymentTransactionService paymentTransactionService;

    @PostMapping("/get/all")
    public ResponseEntity<ResponseErrorTemplate> getTransactions(@RequestBody GetPaymentTransactionRequest request) {
        return ResponseEntity.ok(paymentTransactionService.getTransactions(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseErrorTemplate> getTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentTransactionService.getTransactionById(id));
    }

    @GetMapping("/no/{transactionNo}")
    public ResponseEntity<ResponseErrorTemplate> getTransactionByNo(@PathVariable String transactionNo) {
        return ResponseEntity.ok(paymentTransactionService.getTransactionByNo(transactionNo));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ResponseErrorTemplate> getTransactionsByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentTransactionService.getTransactionsByOrder(orderId));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ResponseErrorTemplate> getTransactionsByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(paymentTransactionService.getTransactionsByCustomer(customerId));
    }

    @GetMapping("/{id}/status-history")
    public ResponseEntity<ResponseErrorTemplate> getTransactionStatusHistory(@PathVariable Long id) {
        return ResponseEntity.ok(paymentTransactionService.getTransactionStatusHistory(id));
    }

    @PostMapping
    public ResponseEntity<ResponseErrorTemplate> createTransaction(@Valid @RequestBody PaymentTransactionRequest request) {
        PaymentTransactionResponse response = paymentTransactionService.createTransaction(request);
        return ResponseEntity.ok(ResponseErrorTemplate.success("Payment transaction created successfully", response));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ResponseErrorTemplate> updateTransactionStatus(
            @PathVariable Long id,
            @Valid @RequestBody PaymentTransactionStatusUpdateRequest request) {
        PaymentTransactionResponse response = paymentTransactionService.updateTransactionStatus(id, request);
        return ResponseEntity.ok(ResponseErrorTemplate.success("Payment transaction status updated successfully", response));
    }
}