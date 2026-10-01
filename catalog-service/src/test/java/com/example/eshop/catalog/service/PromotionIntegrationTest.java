package com.example.eshop.catalog.service;

import com.example.eshop.catalog.model.*;
import com.example.eshop.catalog.enumeration.*;
import com.example.eshop.catalog.dto.request.*;
import com.example.eshop.catalog.repository.*;
import com.example.eshop.catalog.service.impl.*;
import com.example.eshop.catalog.mapper.ProductMapper;
import com.example.eshop.common.dto.*;
import com.example.eshop.common.exception.BusinessLogicException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false, properties = {
    "spring.datasource.url=jdbc:h2:mem:promotions;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PromotionIntegrationTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PromotionIntegrationTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026,10,1,12,0);
    @Configuration @EntityScan({"com.example.eshop.catalog.model", "com.example.eshop.common.audit"})
    @EnableJpaRepositories({"com.example.eshop.catalog.repository", "com.example.eshop.common.audit"})
    @Import({com.example.eshop.common.audit.AuditLogService.class, com.example.eshop.common.audit.AuditContextProvider.class, com.example.eshop.common.audit.AuditSnapshots.class, PromotionService.class, PromotionPricingService.class, CatalogCheckoutService.class,
        ProductResponseService.class, ProductMapper.class, PromotionLifecycleService.class})
    static class Config {
        @Bean Clock clock() { return Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC); }
        @Bean ObjectMapper json() { return new ObjectMapper().findAndRegisterModules(); }
        @Bean TransactionTemplate transactions(PlatformTransactionManager m) { return new TransactionTemplate(m); }
    }
    @Autowired EntityManager em;
    @Autowired TransactionTemplate tx;
    @Autowired PromotionService promotions;
    @Autowired PromotionRepository repository;
    @Autowired PromotionUsageRepository usages;
    @Autowired PromotionPricingService pricing;
    @Autowired PromotionLifecycleService lifecycle;
    @Autowired CatalogCheckoutService checkout;
    @Autowired InventoryRepository inventories;
    @Autowired ProductResponseService responses;
    private static final java.util.concurrent.atomic.AtomicLong ORDER = new java.util.concurrent.atomic.AtomicLong(100);

    private Long sku() {
        return tx.execute(t -> {
            Product product = new Product(); product.setName("Product"); product.setIsActive(true); em.persist(product);
            ProductSku sku = new ProductSku(); sku.setProduct(product); sku.setSku(UUID.randomUUID().toString());
            sku.setDescription("SKU"); sku.setPrice(new BigDecimal("100")); em.persist(sku);
            Inventory i = new Inventory(); i.setProductSku(sku); i.setQuantity(20L); i.setReservedQuantity(0L);
            i.setAvailableQuantity(20L); em.persist(i);
            return sku.getId();
        });
    }
    private Long promotion(Long sku, Long global, Long perUser) {
        var r = promotions.create(new PromotionRequest("Sale", UUID.randomUUID().toString(), null,
            PromotionType.FLASH_SALE, DiscountType.PERCENTAGE, new BigDecimal("20"), null, null,
            NOW, NOW.plusHours(1), 0, global, perUser, false));
        promotions.assign(r.id(), new PromotionSkuRequest(List.of(sku)));
        promotions.transition(r.id(), "activate");
        return r.id();
    }
    private CheckoutRequest request(long order, long user, Long sku, long quantity) {
        return new CheckoutRequest(order, user, List.of(new CheckoutRequest.Line(sku, quantity)));
    }

    @Test void creationAssignmentLifecycleAndProductIntegration() {
        Long sku = sku(); Long id = promotion(sku, null, null);
        promotions.assign(id, new PromotionSkuRequest(List.of(sku)));
        assertThat(promotions.assigned(id,1,20).getTotalElements()).isEqualTo(1);
        assertThat(pricing.calculatePrice(sku).finalPrice()).isEqualByComparingTo("80");
        assertThat(promotions.products(id, 1, 20).getContent()).hasSize(1);
        tx.executeWithoutResult(t -> {
            ProductSku s = em.find(ProductSku.class, sku);
            assertThat(responses.toProductResponse(s.getProduct()).getSkus().get(0).getFinalPrice()).isEqualByComparingTo("80");
            assertThat(s.getPrice()).isEqualByComparingTo("100");
        });
        promotions.transition(id,"disable");
        assertThat(pricing.calculatePrice(sku).promotionApplied()).isFalse();
    }
    @Test void reserveConfirmRetryAndReleaseAreIdempotentWithSnapshots() {
        Long sku=sku(); Long promotion=promotion(sku,null,null); long order=ORDER.incrementAndGet();
        var request=request(order,1,sku,2);
        assertThat(checkout.reserve(request).total()).isEqualByComparingTo("160");
        checkout.reserve(request);
        assertThat(usages.countByPromotionIdAndReleasedFalse(promotion)).isEqualTo(1);
        promotions.transition(promotion,"disable");
        assertThat(checkout.confirm(order).items().get(0).finalPrice()).isEqualByComparingTo("80");
        checkout.confirm(order);
        assertThat(inventories.findByProductSkuId(sku).orElseThrow().getQuantity()).isEqualTo(18);
        checkout.release(order); checkout.release(order);
        assertThat(inventories.findByProductSkuId(sku).orElseThrow().getQuantity()).isEqualTo(20);
        assertThat(usages.countByPromotionIdAndReleasedFalse(promotion)).isZero();
        assertThatThrownBy(() -> checkout.reserve(request)).isInstanceOf(BusinessLogicException.class);
    }
    @Test void perCustomerAndZeroLimitsAndStockRollback() {
        Long sku=sku(); Long id=promotion(sku,null,1L);
        checkout.reserve(request(ORDER.incrementAndGet(),1,sku,1));
        assertThat(checkout.reserve(request(ORDER.incrementAndGet(),1,sku,1)).total()).isEqualByComparingTo("100");
        assertThat(checkout.reserve(request(ORDER.incrementAndGet(),2,sku,1)).total()).isEqualByComparingTo("80");
        assertThatThrownBy(() -> checkout.reserve(request(ORDER.incrementAndGet(),3,sku,100))).isInstanceOf(BusinessLogicException.class);
        assertThat(usages.countByPromotionIdAndReleasedFalse(id)).isEqualTo(2);
        Long other=sku(); promotion(other,0L,null);
        assertThat(pricing.calculatePrice(other).promotionApplied()).isFalse();
    }
    @Test void simultaneousOrdersCannotBothClaimFinalUsage() throws Exception {
        Long sku=sku(); Long id=promotion(sku,1L,null);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        CountDownLatch start=new CountDownLatch(1);
        try {
            var a=pool.submit(() -> { start.await(); return checkout.reserve(request(ORDER.incrementAndGet(),11,sku,1)); });
            var b=pool.submit(() -> { start.await(); return checkout.reserve(request(ORDER.incrementAndGet(),12,sku,1)); });
            start.countDown();
            var totals=List.of(a.get(20,TimeUnit.SECONDS).total(),b.get(20,TimeUnit.SECONDS).total());
            assertThat(totals).usingElementComparator(BigDecimal::compareTo).containsExactlyInAnyOrder(new BigDecimal("80"),new BigDecimal("100"));
            assertThat(usages.countByPromotionIdAndReleasedFalse(id)).isEqualTo(1);
            assertThat(inventories.findByProductSkuId(sku).orElseThrow().getReservedQuantity()).isEqualTo(2);
        } finally { pool.shutdownNow(); }
    }
    @Test void delayedSchedulerAndExactEndBoundary() {
        Long sku=sku(); Long id=promotion(sku,null,null);
        tx.executeWithoutResult(t -> {
            Promotion p=repository.findById(id).orElseThrow(); p.setStatus(PromotionStatus.SCHEDULED);
        });
        assertThat(pricing.calculatePrice(sku).promotionApplied()).isTrue();
        lifecycle.synchronize();
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(PromotionStatus.ACTIVE);
        tx.executeWithoutResult(t -> repository.findById(id).orElseThrow().setEndAt(NOW));
        assertThat(pricing.calculatePrice(sku).promotionApplied()).isFalse();
        lifecycle.synchronize(); lifecycle.synchronize();
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(PromotionStatus.EXPIRED);
    }

    @Autowired com.example.eshop.common.audit.AuditLogRepository auditLogs;
    @Test void promotionMutationsProduceCorrelatedAuditTrail() {
        org.slf4j.MDC.put("requestId", "req_promotion_audit");
        try {
            Long sku = sku();
            Long id = promotion(sku, null, null);
            promotions.remove(id, sku);
            promotions.transition(id, "disable");
            var filter = new com.example.eshop.common.audit.AuditSearch("req_promotion_audit", null,
                null, null, null, "PROMOTION", id.toString(), null, null, null, null);
            var events = auditLogs.findAll(filter.specification(), org.springframework.data.domain.Sort.by("id"));
            assertThat(events).extracting(com.example.eshop.common.audit.AuditLog::getAction).containsExactly(
                com.example.eshop.common.audit.AuditAction.PROMOTION_CREATE,
                com.example.eshop.common.audit.AuditAction.PROMOTION_SKU_ADD,
                com.example.eshop.common.audit.AuditAction.PROMOTION_ACTIVATE,
                com.example.eshop.common.audit.AuditAction.PROMOTION_SKU_REMOVE,
                com.example.eshop.common.audit.AuditAction.PROMOTION_DISABLE);
            assertThat(events.get(4).getOldValue()).contains("ACTIVE");
            assertThat(events.get(4).getNewValue()).contains("DISABLED");
        } finally { org.slf4j.MDC.clear(); }
    }
}

