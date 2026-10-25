package com.example.eshop.payment.dto.response;
import java.math.BigDecimal;
public record PaymentReportBreakdown(Integer month, String currency, String method, String status, Long count, BigDecimal amount) {}
