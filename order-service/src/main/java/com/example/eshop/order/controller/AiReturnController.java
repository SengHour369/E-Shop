package com.example.eshop.order.controller;

import com.example.eshop.common.security.CurrentActor;
import com.example.eshop.common.security.LivePermissionService;
import com.example.eshop.order.model.Return;
import com.example.eshop.order.repository.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/internal/ai/returns")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiReturnController {

    private final ReturnRequestRepository returns;
    private final LivePermissionService permissions;

    @GetMapping
    public List<ReturnCard> mine() {
        permissions.current();
        return returns.findByCustomerId(CurrentActor.userId(), page())
                .stream()
                .map(ReturnCard::from)
                .toList();
    }

    @GetMapping("/admin")
    public List<ReturnCard> admin(@RequestParam(required = false) String status) {
        permissions.require("RETURN_VIEW_ALL");
        return returns.findForAssistant(status, page())
                .stream()
                .map(ReturnCard::from)
                .toList();
    }

    private static PageRequest page() {
        return PageRequest.of(0, 20, Sort.by(Sort.Order.desc("requestedAt"), Sort.Order.desc("id")));
    }

    public record ReturnCard(
            String returnId,
            Long orderId,
            String status,
            BigDecimal amount,
            LocalDateTime requestedAt) {

        static ReturnCard from(Return value) {
            return new ReturnCard(value.getReturnId(), value.getOrderId(), value.getStatus(),
                    value.getAmount(), value.getRequestedAt());
        }
    }
}
