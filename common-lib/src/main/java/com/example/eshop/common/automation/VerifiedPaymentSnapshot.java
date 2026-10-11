package com.example.eshop.common.automation;
import java.math.BigDecimal;
import java.time.Instant;
public record VerifiedPaymentSnapshot(long id,long orderId,long userId,BigDecimal amount,String currency,
        String status,String providerHash,Instant verifiedAt,Instant expiresAt) {}
