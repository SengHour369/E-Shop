package com.example.eshop.common.automation;
import java.math.BigDecimal;
import java.time.Instant;
public record VerifiedPaymentSnapshot(long id,long orderId,Long userId,BigDecimal amount,String currency,
        String status,String providerHash,Instant verifiedAt,Instant expiresAt) {
 public VerifiedPaymentSnapshot(long id,long orderId,int userId,BigDecimal amount,String currency,String status,String providerHash,Instant verifiedAt,Instant expiresAt) { this(id,orderId,Long.valueOf(userId),amount,currency,status,providerHash,verifiedAt,expiresAt); }
}
