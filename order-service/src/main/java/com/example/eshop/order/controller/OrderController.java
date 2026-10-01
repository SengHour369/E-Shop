package com.example.eshop.order.controller;

import com.example.eshop.order.service.OrderService;
import com.example.eshop.order.dto.request.GetOrderRequest;
import com.example.eshop.order.dto.request.OrderRequest;
import com.example.eshop.order.dto.response.ResponseErrorTemplate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Bakong/KHQR payment endpoints (bakong/initiate, bakong/verify, bakong/callback,
 * user/from-cart/bakong) were dropped here — that orchestration now belongs to payment-service.
 */
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

    @PostMapping("/get/all")
    public ResponseEntity<ResponseErrorTemplate> getOrders(@RequestBody GetOrderRequest request) {
        ResponseErrorTemplate response = orderService.getOrders(request);
        return ResponseEntity.ok(response);
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

    @PostMapping("/user/detail")
    public ResponseEntity<ResponseErrorTemplate> getOrderDetailByUser(
            @RequestParam Long userId,
            @RequestParam Long orderId) {
        ResponseErrorTemplate order = orderService.getOrderDetailByUserId(userId, orderId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/user/history")
    public ResponseEntity<Page<ResponseErrorTemplate>> getOrderDetailHistory(
            @RequestParam Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 10, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ResponseErrorTemplate> orders = orderService.getOrderDetailHistory(userId, status, startDate, endDate, pageable);
        return ResponseEntity.ok(orders);
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

    @GetMapping("/summary")
    public ResponseEntity<ResponseErrorTemplate> getOrderSummary() {
        return ResponseEntity.ok(orderService.getOrderStatusSummary());
    }

    @PostMapping("/items")
    public ResponseEntity<ResponseErrorTemplate> getOrderItems(@RequestParam Long orderId) {
        return ResponseEntity.ok(orderService.getOrderItemsByOrderId(orderId));
    }

    @PostMapping("/user/from-cart")
    public ResponseEntity<ResponseErrorTemplate> createOrderFromCart(
            @RequestParam Long userId,
            @Valid @RequestBody OrderRequest request) {
        ResponseErrorTemplate order = orderService.createOrderFromCart(userId, request);
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }
}
