package com.example.eshop.payment;

import com.example.eshop.payment.model.Payment;
import com.example.eshop.payment.repository.PaymentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AiRevenueRepositoryTest.Config.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:ai_revenue;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@Transactional
class AiRevenueRepositoryTest {

    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Payment.class)
    @EnableJpaRepositories(basePackageClasses = PaymentRepository.class)
    static class Config {
    }

    @Autowired
    EntityManager entities;

    @Autowired
    PaymentRepository payments;

    @Test
    void totalsSeparateCurrenciesAndExcludeDeletedFailedAndOtherDays() {
        LocalDate day = LocalDate.of(2026, 10, 9);
        persist("USD", "0.10", "COMPLETED", false, day);
        persist("USD", "0.20", "COMPLETED", false, day);
        persist("KHR", "4000", "COMPLETED", false, day);
        persist("USD", "99", "FAILED", false, day);
        persist("USD", "99", "COMPLETED", true, day);
        persist("USD", "99", "COMPLETED", false, day.plusDays(1));
        var totals = payments.assistantRevenue(day.atStartOfDay(), day.plusDays(1).atStartOfDay());
        assertThat(totals).hasSize(2);
        var usd = totals.stream().filter(value -> "USD".equals(value.currency())).findFirst().orElseThrow();
        assertThat(usd.total()).isEqualByComparingTo("0.30");
        assertThat(usd.paymentCount()).isEqualTo(2);
        var filtered = payments.findVisibleForAssistant("COMPLETED", day.atStartOfDay(),
                day.plusDays(1).atStartOfDay(), org.springframework.data.domain.PageRequest.of(0, 20));
        assertThat(filtered.getTotalElements()).isEqualTo(3);
    }

    private void persist(String currency, String amount, String status, boolean deleted, LocalDate day) {
        var payment = new Payment();
        payment.setPaymentMethod("COD");
        payment.setCurrency(currency);
        payment.setAmount(new BigDecimal(amount));
        payment.setStatus(status);
        payment.setDeleted(deleted);
        payment.setPaymentDate(day.atStartOfDay());
        entities.persist(payment);
    }
}
