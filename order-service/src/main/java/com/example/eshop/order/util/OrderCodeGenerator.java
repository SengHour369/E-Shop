package com.example.eshop.order.util;

import com.example.eshop.common.codes.BusinessCodeSequence;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.stereotype.Component;

@Component
public class OrderCodeGenerator {
    private final BusinessCodeSequence sequence;
    public OrderCodeGenerator(JdbcTemplate jdbc, PlatformTransactionManager manager) {
        sequence = new BusinessCodeSequence(jdbc, manager);
    }
    public String generateOrderCode() { return sequence.next(BusinessCodeSequence.Kind.ORDER); }
}
