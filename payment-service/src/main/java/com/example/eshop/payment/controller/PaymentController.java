package com.example.eshop.payment.controller;

import com.example.eshop.payment.service.impl.PaymentServiceImpl;
import com.example.eshop.payment.dto.request.GetPaymentRequest;
import com.example.eshop.payment.dto.response.PaymentResponse;
import com.example.eshop.payment.dto.response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController extends BaseController {

    private final PaymentServiceImpl paymentService;

    @PostMapping("/user/id")
    public ResponseEntity<ResponseErrorTemplate> getPaymentsByUser(@RequestParam Long userId) {
        List<PaymentResponse> payments = paymentService.getPaymentsByUserId(userId);
        return ResponseEntity.ok(ResponseErrorTemplate.success("Payments retrieved successfully", payments));
    }

    @PostMapping("/user/detail")
    public ResponseEntity<ResponseErrorTemplate> getPaymentDetailByUser(
            @RequestParam Long userId,
            @RequestParam Long paymentId) {
        PaymentResponse payment = paymentService.getPaymentDetailByUserId(userId, paymentId);
        return ResponseEntity.ok(ResponseErrorTemplate.success("Payment detail retrieved successfully", payment));
    }
    @PostMapping("/get/all") public ResponseEntity<ResponseErrorTemplate> getPayments(
            @RequestBody GetPaymentRequest request) {
        ResponseErrorTemplate response = paymentService.getPayments(request);
        return ResponseEntity.ok(response); }
    @PostMapping("/order/")
    public ResponseEntity<ResponseErrorTemplate> getPaymentByOrder(@RequestParam Long orderId) {
        PaymentResponse payment = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(ResponseErrorTemplate.success("Payment retrieved successfully", payment));
    }

    @PostMapping("/transaction/")
    public ResponseEntity<ResponseErrorTemplate> getPaymentByTransaction(@RequestParam String transactionId) {
        PaymentResponse payment = paymentService.getPaymentByTransactionId(transactionId);
        return ResponseEntity.ok(ResponseErrorTemplate.success("Payment retrieved successfully", payment));
    }

    @PostMapping("/user/history")
    public ResponseEntity<Page<ResponseErrorTemplate>> getPaymentHistory(
            @RequestParam Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 10, sort = "paymentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ResponseErrorTemplate> history = paymentService
                .getPaymentHistory(userId, status, startDate, endDate, pageable)
                .map(p -> ResponseErrorTemplate.success("Payment history", p));
        return ResponseEntity.ok(history);
    }
}
