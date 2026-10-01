package com.example.eshop.order.config;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
@Configuration @EnableScheduling
public class CheckoutConfiguration {
    @Bean public TransactionTemplate checkoutTransactions(PlatformTransactionManager manager) {
        return new TransactionTemplate(manager);
    }
}

