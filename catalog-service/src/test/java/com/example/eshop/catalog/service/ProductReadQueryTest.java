package com.example.eshop.catalog.service;

import com.example.eshop.catalog.mapper.ProductMapper;
import com.example.eshop.catalog.model.*;
import com.example.eshop.catalog.repository.ProductRepository;
import com.example.eshop.catalog.service.impl.ProductResponseService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false, properties = {
    "spring.datasource.url=jdbc:h2:mem:products;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.hibernate.generate_statistics=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = ProductReadQueryTest.Config.class)
class ProductReadQueryTest {
    @org.springframework.context.annotation.Configuration
    @EntityScan("com.example.eshop.catalog.model")
    @EnableJpaRepositories("com.example.eshop.catalog.repository")
    @Import({ProductResponseService.class, ProductMapper.class, com.example.eshop.catalog.service.impl.PromotionPricingService.class})
    static class Config {
        @org.springframework.context.annotation.Bean java.time.Clock clock() { return java.time.Clock.systemUTC(); }
    }

    @Autowired EntityManager em;
    @Autowired ProductRepository products;
    @Autowired ProductResponseService responses;

    @Test void fullPagesUseEightQueriesRegardlessOfProductCount() {
        for (int i = 0; i < 21; i++) seedProduct(i);
        em.flush();
        for (int size : new int[]{2, 20}) {
            em.clear();
            var statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
            statistics.clear();
            var page = products.findAllNotDeleted(PageRequest.of(0, size, Sort.by("id").descending()));
            var result = responses.toProductResponses(page.getContent());
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(8);
            assertThat(result).hasSize(size);
            assertThat(result.get(0).getName()).isEqualTo("Product 20");
            assertThat(result.get(0).getImage()).containsExactly("https://example.test/main-20");
            assertThat(result.get(0).getSkus()).hasSize(2);
            assertThat(result.get(0).getSkus().get(0).getQuantity()).isEqualTo(12L);
            assertThat(result.get(0).getSkus().get(0).getInventory().getAvailableQuantity()).isEqualTo(10L);
            assertThat(result.get(0).getSkus().get(0).getProductAttributeResponse()).hasSize(1);
            assertThat(result.get(0).getSkus().get(0).getProductAttributeResponse().get(0).getAttributes())
                    .extracting(value -> value.getValue()).containsExactly("Blue");
        }
    }

    @Test void emptyPagesDoNotLoadRelatedTables() {
        var statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertThat(responses.toProductResponses(java.util.List.of())).isEmpty();
        assertThat(statistics.getPrepareStatementCount()).isZero();
    }

    private void seedProduct(int index) {
        Product product = new Product();
        product.setName("Product " + index);
        product.setIsActive(true);
        em.persist(product);
        Image main = new Image();
        main.setUrl("https://example.test/main-" + index);
        main.setProduct(product);
        em.persist(main);
        for (int variant = 0; variant < 2; variant++) {
            Image image = new Image();
            image.setUrl("https://example.test/sku-" + index + "-" + variant);
            em.persist(image);
            ProductSku sku = new ProductSku();
            sku.setProduct(product);
            sku.setSku("SKU-" + index + "-" + variant);
            sku.setDescription("Variant");
            sku.setPrice(BigDecimal.TEN);
            sku.setImage(image);
            em.persist(sku);
            ProductAttribute attribute = new ProductAttribute();
            attribute.setName("Color");
            attribute.setProductSkuId(sku.getId());
            em.persist(attribute);
            ProductAttributeValue value = new ProductAttributeValue();
            value.setAttributeId(attribute.getId());
            value.setValue("Blue");
            em.persist(value);
            Inventory inventory = new Inventory();
            inventory.setProductSku(sku);
            inventory.setQuantity(12L);
            inventory.setReservedQuantity(2L);
            inventory.setAvailableQuantity(10L);
            em.persist(inventory);
        }
    }
}
