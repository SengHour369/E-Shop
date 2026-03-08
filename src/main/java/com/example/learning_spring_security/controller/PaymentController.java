//package com.example.learning_spring_security.controller;
//
//import com.example.learning_spring_security.Service.ServiceStructure.PaymentService;
//import com.example.learning_spring_security.dto.Request.PaymentRequest;
//import com.example.learning_spring_security.dto.Response.PaymentResponse;
//import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/v1/payments")
//@RequiredArgsConstructor
//public class PaymentController extends BaseController {
//
//    private final PaymentService paymentService;
//
//    @GetMapping("/order/{orderId}")
//    public ResponseEntity<PaymentResponse> getPaymentByOrderId(@PathVariable Long orderId) {
//        PaymentResponse payment = paymentService.getPaymentByOrderId(orderId);
//        return ResponseEntity.ok(payment);
//    }
//
//    @GetMapping("/transaction/{transactionId}")
//    public ResponseEntity<PaymentResponse> getPaymentByTransactionId(@PathVariable String transactionId) {
//        PaymentResponse payment = paymentService.getPaymentByTransactionId(transactionId);
//        return ResponseEntity.ok(payment);
//    }
//
//    @PostMapping("/order/{orderId}/process")
//    public ResponseEntity<PaymentResponse> processPayment(
//            @PathVariable Long orderId,
//            @Valid @RequestBody PaymentRequest request) {
//        PaymentResponse payment = paymentService.processPayment(orderId, request);
//        return new ResponseEntity<>(payment, HttpStatus.CREATED);
//    }
//
//    @PatchMapping("/{paymentId}/status")
//    public ResponseEntity<PaymentResponse> updatePaymentStatus(
//            @PathVariable Long paymentId,
//            @RequestParam String status,
//            @RequestParam(required = false) String transactionId) {
//        PaymentResponse payment = paymentService.updatePaymentStatus(paymentId, status, transactionId);
//        return ResponseEntity.ok(payment);
//    }
//}