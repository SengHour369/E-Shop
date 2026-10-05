package com.example.eshop.order.dto.response;

import java.math.BigDecimal;

/** Aggregates a complete calendar month; currencies are never combined. */
public record MonthlyReportResponse(Integer month, Long count, BigDecimal amount, String currency) {
}
