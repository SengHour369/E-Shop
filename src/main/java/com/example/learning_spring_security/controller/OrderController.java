package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Service.ServiceStructure.OrderService;
import com.example.learning_spring_security.dto.Request.OrderRequest;
import com.example.learning_spring_security.dto.Response.OrderResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController extends BaseController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<Page<ResponseErrorTemplate>> getAllOrders(
            @PageableDefault(size = 10, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ResponseErrorTemplate> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/user/id/")
    public ResponseEntity<Page<ResponseErrorTemplate>> getUserOrders(
            @RequestParam Long userId,
            @PageableDefault(size = 10, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ResponseErrorTemplate> orders = orderService.getUserOrders(userId, pageable);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/id/")
    public ResponseEntity<ResponseErrorTemplate> getOrderById(@RequestParam Long id) {
        ResponseErrorTemplate order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/number/")
    public ResponseEntity<ResponseErrorTemplate> getOrderByNumber(@RequestParam String orderNumber) {
        ResponseErrorTemplate order = orderService.getOrderByNumber(orderNumber);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/user/from-cart")
    public ResponseEntity<ResponseErrorTemplate> createOrderFromCart(
            @RequestParam Long userId,
            @Valid @RequestBody OrderRequest request) {
        ResponseErrorTemplate order = orderService.createOrderFromCart(userId, request);
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

    @PostMapping("/status/")
    public ResponseEntity<ResponseErrorTemplate> updateOrderStatus(
            @RequestParam Long id,
            @RequestParam String status) {
        ResponseErrorTemplate order = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/user/cancel")
    public ResponseEntity<ResponseErrorTemplate> cancelOrder(
            @RequestParam Long id,
            @RequestParam Long userId) {
        ResponseErrorTemplate order = orderService.cancelOrder(id, userId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/user/from-cart/bakong")
    public ResponseEntity<ResponseErrorTemplate> createOrderWithBakongPayment(
            @RequestParam Long userId,
            @Valid @RequestBody OrderRequest request) {
        ResponseErrorTemplate order = orderService.createOrderWithBakongPayment(userId, request);
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

    @PostMapping("/bakong/initiate")
    public ResponseEntity<ResponseErrorTemplate> initiateBakongPayment(@RequestParam Long orderId) {
        ResponseErrorTemplate response = orderService.initiateBakongPayment(orderId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bakong/verify")
    public ResponseEntity<ResponseErrorTemplate> verifyBakongPayment(
            @RequestParam Long orderId,
            @RequestParam String transactionId) {
        ResponseErrorTemplate response = orderService.verifyBakongPayment(orderId, transactionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bakong/callback")
    public ResponseEntity<ResponseErrorTemplate> processBakongPaymentCallback(
            @RequestParam String orderNumber,
            @RequestParam String transactionId,
            @RequestParam String status) {
        ResponseErrorTemplate response = orderService.processBakongPaymentCallback(orderNumber, transactionId, status);
        return ResponseEntity.ok(response);
    }
}