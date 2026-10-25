package com.example.eshop.payment;

import com.example.eshop.payment.model.Payment;
import com.example.eshop.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.cloud.discovery.enabled=false"})
@org.springframework.test.context.ContextConfiguration(classes=MonthlyReportRepositoryTest.Config.class)
class MonthlyReportRepositoryTest {
    @org.springframework.context.annotation.Configuration
    @org.springframework.boot.autoconfigure.domain.EntityScan("com.example.eshop.payment.model")
    @org.springframework.data.jpa.repository.config.EnableJpaRepositories("com.example.eshop.payment.repository")
    static class Config {}
    @Autowired PaymentRepository repository;

    @Test
    void aggregatesBeyondOnePageAndExcludesOtherYearsDeletedAndInvalidStatuses() {
        for (int i = 0; i < 105; i++) save(i, 2026, "COMPLETED", false, "USD");
        save(200, 2025, "COMPLETED", false, "USD");
        save(201, 2027, "COMPLETED", false, "USD");
        save(202, 2026, "FAILED", false, "USD");
        save(203, 2026, "COMPLETED", true, "USD");
        save(204, 2026, "COMPLETED", false, "KHR");
        repository.flush();
        var rows = repository.monthlyReport(LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2027, 1, 1, 0, 0));
        assertThat(rows).hasSize(2);
        var usd = rows.stream().filter(row -> "USD".equals(row.currency())).findFirst().orElseThrow();
        assertThat(usd.count()).isEqualTo(105);
        assertThat(usd.amount()).isEqualByComparingTo("1050.00");
        assertThat(usd.month()).isEqualTo(1);
    }

    @Test
    void breakdownSeparatesRefundsCurrenciesAndMethods() {
        save(1,2026,"COMPLETED",false,"USD");
        save(2,2026,"REFUNDED",false,"USD");
        save(3,2026,"COMPLETED",false,"KHR");
        save(4,2026,"FAILED",false,"USD");
        save(5,2026,"REFUNDED",true,"USD");
        save(6,2025,"COMPLETED",false,"USD");
        var card=Payment.builder().userId(1L).status("COMPLETED").deleted(false).paymentMethod("CREDIT_CARD").paymentDate(LocalDateTime.of(2026,1,31,23,59)).amount(new BigDecimal("20.00")).currency("USD").build();
        repository.saveAndFlush(card);
        var rows=repository.reportBreakdown(LocalDateTime.of(2026,1,1,0,0),LocalDateTime.of(2027,1,1,0,0));
        assertThat(rows).hasSize(4);
        assertThat(rows.stream().filter(row->"REFUNDED".equals(row.status())).findFirst().orElseThrow().amount()).isEqualByComparingTo("10.00");
        assertThat(rows.stream().filter(row->"CREDIT_CARD".equals(row.method())).findFirst().orElseThrow().amount()).isEqualByComparingTo("20.00");
    }

    private void save(int id, int year, String status, boolean deleted, String currency) {
        var entity = new Payment();
        entity.setUserId(1L);
        entity.setStatus(status);
        entity.setDeleted(deleted);
        entity.setPaymentMethod("COD");
        entity.setPaymentDate(LocalDateTime.of(year, 1, 1, 0, 0));
        entity.setAmount(new BigDecimal("10.00"));
        entity.setCurrency(currency);
        repository.save(entity);
    }
}
