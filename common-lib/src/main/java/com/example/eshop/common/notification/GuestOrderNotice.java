package com.example.eshop.common.notification;
import java.math.BigDecimal;
public record GuestOrderNotice(String verifiedEmail,String orderNumber,String status,BigDecimal amount,String currency,boolean paymentVerified) { }
