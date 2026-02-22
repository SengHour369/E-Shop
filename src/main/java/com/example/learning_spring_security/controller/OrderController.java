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

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ResponseErrorTemplate>> getUserOrders(
            @PathVariable Long userId,
            @PageableDefault(size = 10, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ResponseErrorTemplate> orders = orderService.getUserOrders(userId, pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseErrorTemplate> getOrderById(@PathVariable Long id) {
        ResponseErrorTemplate order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<ResponseErrorTemplate> getOrderByNumber(@PathVariable String orderNumber) {
        ResponseErrorTemplate order = orderService.getOrderByNumber(orderNumber);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/user/{userId}/from-cart")
    public ResponseEntity<ResponseErrorTemplate> createOrderFromCart(
            @PathVariable Long userId,
            @Valid @RequestBody OrderRequest request) {
        ResponseErrorTemplate order = orderService.createOrderFromCart(userId, request);
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ResponseErrorTemplate> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        ResponseErrorTemplate order = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/user/{userId}/cancel")
    public ResponseEntity<ResponseErrorTemplate> cancelOrder(
            @PathVariable Long id,
            @PathVariable Long userId) {
        ResponseErrorTemplate order = orderService.cancelOrder(id, userId);
        return ResponseEntity.ok(order);
    }
}