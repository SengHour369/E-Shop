package com.example.eshop.order.controller;

import com.example.eshop.common.exception.ResourceNotFoundException;
import com.example.eshop.common.security.CurrentCustomer;
import com.example.eshop.order.dto.response.ResponseErrorTemplate;
import com.example.eshop.order.model.Return;
import com.example.eshop.order.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Read-only customer views. Both the return and its order must belong to the caller. */
@RestController
@RequestMapping("/api/v1/returns/user")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerReturnController {

    private final ReturnRequestRepository returns;
    private final OrderRepository orders;
    private final RefundRepository refunds;
    private final ReturnStatusHistoryRepository histories;

    public record Summary(
            String return_id,
            String status,
            String return_type,
            LocalDateTime requested_at,
            BigDecimal amount
    ) {}

    public record History(
            String status,
            LocalDateTime changed_at
    ) {}

    public record RefundView(
            BigDecimal amount,
            String status,
            LocalDateTime requested_at,
            LocalDateTime processed_at
    ) {}

    public record Detail(
            String return_id,
            Long order_id,
            String order_no,
            LocalDateTime order_date,
            Long product_id,
            String return_type,
            String reason,
            String status,
            BigDecimal amount,
            LocalDateTime requested_at,
            List<History> history,
            RefundView refund
    ) {}

    @GetMapping
    public ResponseErrorTemplate list(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page
    ) {
        CurrentCustomer.require(userId);

        if (page < 0) {
            throw new IllegalArgumentException("Invalid page");
        }

        var result = returns.findOwnedByCustomer(
                        userId,
                        PageRequest.of(
                                page,
                                10,
                                Sort.by("requestedAt").descending()
                        )
                )
                .map(r -> new Summary(
                        r.getReturnId(),
                        r.getStatus(),
                        r.getReturnType(),
                        r.getRequestedAt(),
                        r.getAmount()
                ));
        return ResponseErrorTemplate.success(
                "Customer returns retrieved",
                result
        );
    }

    @GetMapping("/{returnId}")
    public ResponseErrorTemplate detail(
            @PathVariable String returnId,
            @RequestParam Long userId
    ) {
        CurrentCustomer.require(userId);
        Return request = returns.findByReturnId(returnId)
                .filter(r -> userId.equals(r.getCustomerId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Return not found"
                ));
        var order = orders.findById(request.getOrderId())
                .filter(o -> userId.equals(o.getUserId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Return not found"
                ));
        var history = histories
                .findByReturnRequestOrderByChangedAtDesc(request.getId())
                .stream()
                .map(h -> new History(h.getNewStatus(), h.getChangedAt()))
                .toList();
        var refund = refunds.findByReturnId(returnId)
                .filter(f ->
                        userId.equals(f.getCustomerId())
                                && order.getId().equals(f.getOrderId())
                )
                .map(f -> new RefundView(
                        f.getAmount(),
                        f.getStatus(),
                        f.getRequestedAt(),
                        f.getProcessedAt()
                ))
                .orElse(null);
        return ResponseErrorTemplate.success(
                "Customer return retrieved",
                new Detail(
                        request.getReturnId(),
                        order.getId(),
                        order.getOrderNumber(),
                        order.getOrderDate(),
                        request.getProductId(),
                        request.getReturnType(),
                        request.getReason(),
                        request.getStatus(),
                        request.getAmount(),
                        request.getRequestedAt(),
                        history,
                        refund
                )
        );
    }
}