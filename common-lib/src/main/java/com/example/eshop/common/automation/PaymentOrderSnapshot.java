package com.example.eshop.common.automation;
import java.math.BigDecimal;
public record PaymentOrderSnapshot(long orderId,long userId,BigDecimal amount,String currency,String status,String method) {}
