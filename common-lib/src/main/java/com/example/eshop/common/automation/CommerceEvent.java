package com.example.eshop.common.automation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Versioned, minimal business facts. A message is never proof that money was received. */
public record CommerceEvent(
        int version,
        UUID eventId,
        Type type,
        long orderId,
        Long userId,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        Long paymentId,
        String requestId,
        String traceId,
        Instant occurredAt
) {

    public CommerceEvent(int version,UUID eventId,Type type,long orderId,int userId,BigDecimal amount,
            String currency,String paymentMethod,Long paymentId,String requestId,String traceId,Instant occurredAt) {
        this(version,eventId,type,orderId,Long.valueOf(userId),amount,currency,paymentMethod,paymentId,requestId,traceId,occurredAt);
    }

    public static final String ORDERS = "commerce.orders.v1";
    public static final String PAYMENTS = "commerce.payments.v1";

    public enum Type {
        ORDER_CREATED,
        ORDER_CANCELLED,
        ORDER_STATUS_CHANGED,
        PAYMENT_VERIFIED,
        PAYMENT_EXPIRED,
        PAYMENT_FAILED,
        REVIEW_REQUIRED
    }

    public void validate() {
        if ((version != 1 && version != 2)
                || eventId == null
                || type == null
                || orderId <= 0
                || (userId == null ? version != 2 : userId <= 0)
                || amount == null
                || amount.signum() < 0
                || !"USD".equals(currency)
                || requestId == null
                || !requestId.matches("[A-Za-z0-9._:-]{1,128}")
                || occurredAt == null
                || paymentMethod == null
                || paymentMethod.length() > 30
                || traceId != null
                && !traceId.matches("[a-f0-9]{32}")) {
            throw new IllegalArgumentException("Invalid commerce event");
        }

        if (type.name().startsWith("PAYMENT_")
                && (paymentId == null || paymentId <= 0)) {
            throw new IllegalArgumentException(
                    "Payment event requires payment id"
            );
        }
    }
}