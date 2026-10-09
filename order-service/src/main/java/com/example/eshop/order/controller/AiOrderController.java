package com.example.eshop.order.controller;

import com.example.eshop.common.exception.ResourceNotFoundException;
import com.example.eshop.common.security.CurrentActor;
import com.example.eshop.common.security.LivePermissionService;
import com.example.eshop.order.model.OrderDetail;
import com.example.eshop.order.repository.OrderRepository;
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
@RequestMapping("/internal/ai/orders")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiOrderController {

    private final OrderRepository orders;
    private final LivePermissionService permissions;
    private final com.example.eshop.order.service.OrderService orderService;

    @GetMapping("/{number}/cancel-preview")
    public Object cancelPreview(@PathVariable String number) {
        OrderCard order = get(number);
        return java.util.Map.of("order", order, "eligible", "PENDING".equals(order.status()));
    }

    @org.springframework.web.bind.annotation.PostMapping("/{number}/cancel")
    @Transactional
    public OrderCard cancel(@PathVariable String number) {
        OrderCard owned = get(number);
        orderService.cancelOrder(owned.id(), CurrentActor.userId());
        return get(number);
    }

    @GetMapping("/{number}")
    public OrderCard get(@PathVariable String number) {
        permissions.current();
        long userId = CurrentActor.userId();
        return orders.findByOrderNumber(number)
                .filter(order -> Long.valueOf(userId).equals(order.getUserId()))
                .filter(order -> !Boolean.TRUE.equals(order.getDeleted()))
                .map(OrderCard::from)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    @GetMapping
    public List<OrderCard> mine() {
        permissions.current();
        return orders.findVisibleByUserId(CurrentActor.userId(), page())
                .stream()
                .map(OrderCard::from)
                .toList();
    }

    @GetMapping("/latest")
    public OrderCard latest() {
        return mine().stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    @GetMapping("/admin")
    public List<OrderCard> admin(@RequestParam(required = false) String status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        permissions.require("ORDER_VIEW_ALL");
        return orders.findVisibleForAssistant(status, date == null ? null : date.atStartOfDay(),
                        date == null ? null : date.plusDays(1).atStartOfDay(), page())
                .stream()
                .map(OrderCard::from)
                .toList();
    }

    @GetMapping("/summary")
    public Object summary() {
        permissions.require("REPORT_VIEW");
        return orders.assistantStatusSummary();
    }

    private static PageRequest page() {
        return PageRequest.of(0, 20, Sort.by(Sort.Order.desc("orderDate"), Sort.Order.desc("id")));
    }

    public record OrderCard(
            Long id,
            String orderNumber,
            String status,
            BigDecimal totalAmount,
            LocalDateTime orderDate) {

        static OrderCard from(OrderDetail order) {
            return new OrderCard(order.getId(), order.getOrderNumber(), order.getStatus(),
                    order.getTotalAmount(), order.getOrderDate());
        }
    }
}
