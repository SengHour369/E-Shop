package com.example.eshop.payment.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import com.example.eshop.payment.bakong.KhqrType;

/** Configuration availability, not a promise that a bank is online or a payment is settled. */
@RestController
public class PaymentMethodsController {
    private final boolean khqrConfigured;
    public PaymentMethodsController(@Value("${automation.enabled:false}") boolean enabled,
            @Value("${bakong.account-id:}") String account,
            @Value("${automation.merchant-name:}") String name,
            @Value("${automation.merchant-id:}") String merchant,
            @Value("${automation.acquiring-bank:}") String bank,
            @Value("${bakong.khqr-type:MERCHANT}") KhqrType type) {
        khqrConfigured = enabled && type.configured(account, name, merchant, bank);
    }
    @GetMapping("/api/v1/payments/methods")
    public List<String> methods() {
        return khqrConfigured ? List.of("CASH_ON_DELIVERY", "BAKONG", "ABA", "KHQR") : List.of("CASH_ON_DELIVERY");
    }
}
