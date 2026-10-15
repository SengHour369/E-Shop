package com.example.eshop.order.automation;

import com.example.eshop.common.automation.PaymentOrderSnapshot;
import com.example.eshop.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "automation.enabled", havingValue = "true")
public class CommerceOrderController {

    private final OrderRepository orders;
    private final OrderReviewService reviews;

    @PostMapping("/api/v1/orders/automation/orders/{id}/review")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Object review(@PathVariable long id) {
        return reviews.review(id);
    }

    @GetMapping("/internal/ai/orders/automation/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Object aiReview(@PathVariable long id) {
        return reviews.inspect(id);
    }

    @GetMapping("/internal/commerce/orders/{id}")
    @PreAuthorize("hasAuthority('SERVICE_PAYMENT')")
    @Transactional(readOnly = true)
    public PaymentOrderSnapshot get(@PathVariable long id) {
        var o = orders.findById(id).orElseThrow();
        return new PaymentOrderSnapshot(
                id,
                o.getUserId(),
                o.getTotalAmount(),
                "USD",
                o.getStatus(),
                o.getPaymentMethod()
        );
    }
}