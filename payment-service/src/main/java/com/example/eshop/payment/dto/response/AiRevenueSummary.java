package com.example.eshop.payment.dto.response;

import java.math.BigDecimal;

/** Gross completed payments, kept separate for each currency. */
public record AiRevenueSummary(String currency, Long paymentCount, BigDecimal total) {
}
