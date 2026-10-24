package com.example.eshop.order.automation;

import com.example.eshop.common.exception.BusinessLogicException;
import com.example.eshop.order.model.OrderDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/** A local timeout or provider outage cannot authorize releasing a payable reservation. */
@Component
@RequiredArgsConstructor
public class QrCancellationGuard {

    private final ObjectProvider<PaymentAutomationClient> payments;

    public void requireUnpaid(OrderDetail order) {
        if (!Set.of("ABA", "KHQR", "BAKONG").contains(
                Objects.toString(order.getPaymentMethod(), "")
        )) {
            return;
        }

        var client = payments.getIfAvailable();

        if (client == null) {
            throw pending();
        }

        var payment = client.forOrder(order.getId());

        if (payment == null
                || payment.orderId() != order.getId()
                || !Objects.equals(payment.userId(), order.getUserId())
                || payment.amount() == null
                || payment.amount().compareTo(order.getTotalAmount()) != 0
                || !"USD".equals(payment.currency())
                || !"FAILED".equals(payment.status())
                || payment.verifiedAt() != null
                || payment.providerHash() != null
                || payment.expiresAt() == null
                || Instant.now().isBefore(payment.expiresAt())) {
            throw pending();
        }
    }

    private BusinessLogicException pending() {
        return new BusinessLogicException(
                "Payment reconciliation is pending. "
                        + "Do not pay again or cancel this order yet."
        );
    }
}