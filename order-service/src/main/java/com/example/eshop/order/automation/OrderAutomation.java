package com.example.eshop.order.automation;

import com.example.eshop.common.automation.*;
import com.example.eshop.common.notification.NotificationEvent;
import com.example.eshop.order.repository.OrderRepository;
import com.example.eshop.order.service.impl.CatalogAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "automation.enabled", havingValue = "true")
public class OrderAutomation implements CommerceHandler {

    private final OrderRepository orders;
    private final PaymentAutomationClient payments;
    private final CatalogAccess catalog;
    private final AutomationEvents events;
    private final com.example.eshop.common.audit.AuditLogService audit;

    @Override
    @Transactional
    public void handle(CommerceEvent event) {
        if (event.type() != CommerceEvent.Type.PAYMENT_VERIFIED
                && event.type() != CommerceEvent.Type.PAYMENT_EXPIRED
                && event.type() != CommerceEvent.Type.PAYMENT_FAILED) {
            return;
        }

        var payment = payments.get(event.paymentId());
        var order = orders.lockById(event.orderId()).orElseThrow();

        if (!java.util.Objects.equals(event.userId(),order.getUserId())
                || payment == null
                || payment.id() != event.paymentId()
                || payment.orderId() != order.getId()
                || !java.util.Objects.equals(payment.userId(),order.getUserId())
                || !"USD".equals(payment.currency())
                || payment.amount().compareTo(order.getTotalAmount()) != 0) {
            throw new IllegalStateException(
                    "Authoritative payment/order mismatch"
            );
        }

        if (event.type() == CommerceEvent.Type.PAYMENT_VERIFIED) {
            if (!"SUCCESS".equals(payment.status())
                    || payment.verifiedAt() == null
                    || payment.providerHash() == null) {
                throw new IllegalStateException(
                        "Missing provider verification"
                );
            }

            if (java.util.Set.of("PROCESSING", "SHIPPED", "DELIVERED")
                    .contains(order.getStatus())
                    && java.util.Objects.equals(
                    order.getPaymentId(),
                    payment.id()
            )) {
                return;
            }

            if (!"PENDING".equals(order.getStatus())) {
                events.notify(event, NotificationEvent.Type.PAYMENT_SUCCESS);
                events.review(
                        order.getId(),
                        "PAID_ORDER_STATE_CONFLICT",
                        "Payment is verified but the order is not pending. "
                                + "Do not revive a cancelled order; "
                                + "review compensation/refund."
                );
                return;
            }

            OrderTransitions.require(order.getStatus(), "CONFIRMED", true);
            OrderTransitions.require("CONFIRMED", "PROCESSING", true);

            // Catalog is idempotent by order id, including crash after this call.
            catalog.confirm(order.getId());

            order.setPaymentId(payment.id());
            order.setStatus("PROCESSING");
            order.setCatalogCompleted(true);

            audit.record(
                    new com.example.eshop.common.audit.AuditEvent(
                            com.example.eshop.common.audit.AuditAction.UPDATE,
                            "ORDER",
                            Long.toString(order.getId()),
                            null,
                            java.util.Map.of(
                                    "status", "PROCESSING",
                                    "paymentId", payment.id()
                            ),
                            com.example.eshop.common.audit.AuditResult.SUCCESS,
                            null
                    )
            );
            events.notify(event, NotificationEvent.Type.PAYMENT_SUCCESS);
        } else {
            if (!"FAILED".equals(payment.status())
                    || payment.verifiedAt() != null
                    || payment.expiresAt() == null
                    || java.time.Instant.now().isBefore(payment.expiresAt())) {
                throw new IllegalStateException(
                        "Unverified negative payment outcome"
                );
            }

            if (!"PENDING".equals(order.getStatus())) {
                return;
            }

            OrderTransitions.require(order.getStatus(), "CANCELLED", false);
            catalog.release(order.getId());
            order.setStatus("CANCELLED");
            order.setCatalogCompleted(true);
            events.notify(event, NotificationEvent.Type.PAYMENT_FAILED);
            events.notify(event, NotificationEvent.Type.ORDER_CANCELLED);
            events.emit(
                    CommerceEvent.ORDERS,
                    AutomationEvents.event(
                            CommerceEvent.Type.ORDER_CANCELLED,
                            order.getId(),
                            order.getUserId(),
                            order.getTotalAmount(),
                            order.getPaymentMethod(),
                            payment.id()
                    )
            );
        }
    }
}