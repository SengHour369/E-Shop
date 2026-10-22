package com.example.eshop.common.automation;
import java.math.BigDecimal;
public record PaymentOrderSnapshot(long orderId,Long userId,BigDecimal amount,String currency,String status,String method) {
 public PaymentOrderSnapshot(long orderId,int userId,BigDecimal amount,String currency,String status,String method) { this(orderId,Long.valueOf(userId),amount,currency,status,method); }
}
