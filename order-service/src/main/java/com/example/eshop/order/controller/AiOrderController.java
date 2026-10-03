package com.example.eshop.order.controller;
import com.example.eshop.order.repository.OrderRepository;
import com.example.eshop.order.service.OrderService;
import com.example.eshop.common.security.CurrentActor;
import com.example.eshop.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
@RestController @RequestMapping("/internal/ai/orders") @RequiredArgsConstructor
public class AiOrderController {
    private final OrderRepository orders;
    private final OrderService service;
    @GetMapping("/{number}") @Transactional(readOnly=true)
    public Object get(@PathVariable String number) {
        long userId = CurrentActor.userId();
        var order = orders.findByOrderNumber(number).filter(o -> Long.valueOf(userId).equals(o.getUserId()))
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return service.getOrderDetailByUserId(userId, order.getId());
    }
}
