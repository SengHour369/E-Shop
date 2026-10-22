package com.example.eshop.payment.util;

import com.example.eshop.common.codes.BusinessCodeSequence;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.stereotype.Component;

@Component
public class PaymentCodeGenerator {
    private final BusinessCodeSequence sequence;
    public PaymentCodeGenerator(JdbcTemplate jdbc, PlatformTransactionManager manager) {
        sequence = new BusinessCodeSequence(jdbc, manager);
    }
    public String generatePaymentCode() { return sequence.next(BusinessCodeSequence.Kind.PAYMENT); }
}
