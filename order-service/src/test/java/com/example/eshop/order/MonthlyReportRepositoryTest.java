package com.example.eshop.order;

import com.example.eshop.order.model.OrderDetail;
import com.example.eshop.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.cloud.discovery.enabled=false"})
@org.springframework.test.context.ContextConfiguration(classes=MonthlyReportRepositoryTest.Config.class)
class MonthlyReportRepositoryTest {
    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    @org.springframework.boot.autoconfigure.domain.EntityScan("com.example.eshop.order.model")
    @org.springframework.data.jpa.repository.config.EnableJpaRepositories("com.example.eshop.order.repository")
    static class Config {}
    @Autowired OrderRepository repository;
    @Autowired com.example.eshop.order.repository.OrderItemRepository items;

    @Test
    void aggregatesBeyondOnePageAndExcludesOtherYearsDeletedAndInvalidStatuses() {
        for (int i = 0; i < 105; i++) save(i, 2026, "DELIVERED", false, "USD");
        save(200, 2025, "DELIVERED", false, "USD");
        save(201, 2027, "DELIVERED", false, "USD");
        save(202, 2026, "FAILED", false, "USD");
        save(203, 2026, "DELIVERED", true, "USD");
        
        repository.flush();
        var rows = repository.monthlyReport(LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2027, 1, 1, 0, 0));
        assertThat(rows).hasSize(1);
        var usd = rows.stream().filter(row -> "USD".equals(row.currency())).findFirst().orElseThrow();
        assertThat(usd.count()).isEqualTo(105);
        assertThat(usd.amount()).isEqualByComparingTo("1050.00");
        assertThat(usd.month()).isEqualTo(1);
    }

    @Test void itemAggregatesUseTheSameYearStatusAndDeletionFilters(){
        for(int i=0;i<6;i++){
            save(500+i,i==4?2025:2026,i==2?"CANCELLED":i==3?"CHECKOUT_PENDING":"DELIVERED",i==5,"USD");
            var order=repository.findByOrderNumber("REPORT-"+(500+i)).orElseThrow();
            items.save(com.example.eshop.order.model.OrderItem.builder().orderDetail(order).productSkuId(10L).quantity(2L).unitPrice(new BigDecimal("5.00")).totalPrice(new BigDecimal("10.00")).build());
        }
        items.flush();var start=LocalDateTime.of(2026,1,1,0,0);var result=items.salesBySku(start,start.plusYears(1));assertThat(result).hasSize(1);assertThat(result.get(0).items()).isEqualTo(4);assertThat(result.get(0).amount()).isEqualByComparingTo("20.00");assertThat(items.monthlyItems(start,start.plusYears(1)).get(0).items()).isEqualTo(4);
    }

    private void save(int id, int year, String status, boolean deleted, String currency) {
        var entity = new OrderDetail();
        entity.setUserId(1L);
        entity.setStatus(status);
        entity.setDeleted(deleted);
        entity.setOrderNumber("REPORT-" + id);
        entity.setOrderDate(LocalDateTime.of(year, 1, 1, 0, 0));
        entity.setTotalAmount(new BigDecimal("10.00"));
        repository.save(entity);
    }
}
