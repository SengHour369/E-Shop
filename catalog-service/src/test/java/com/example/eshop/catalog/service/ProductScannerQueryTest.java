package com.example.eshop.catalog.service;

import com.example.eshop.catalog.model.Product;
import com.example.eshop.catalog.model.ProductSku;
import com.example.eshop.catalog.repository.ProductRepository;
import com.example.eshop.catalog.repository.ProductSkuRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:scanner;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = ProductScannerQueryTest.Config.class)
class ProductScannerQueryTest {
    @org.springframework.context.annotation.Configuration
    @EntityScan("com.example.eshop.catalog.model")
    @EnableJpaRepositories("com.example.eshop.catalog.repository")
    static class Config {}

    @Autowired EntityManager em;
    @Autowired ProductSkuRepository skus;
    @Autowired ProductRepository products;

    @Test void barcodeLookupIsOneIndexedQuery() {
        for (int i = 0; i < 5; i++) persist("SKU-" + i, i == 2 ? "8850123456787" : null);
        em.flush();
        em.clear();
        Statistics statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var found = skus.findByBarcodes(java.util.List.of("8850123456787"));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        assertThat(java.util.Arrays.toString(statistics.getQueries())).contains("barcode");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getSku()).isEqualTo("SKU-2");
        assertThat(found.get(0).getProduct().getName()).isEqualTo("Nike Air Max");
    }

    @Test void skuLookupAndNameSearchDoNotScanByLoadingEverySku() {
        persist("NIKE-BLK-42", null);
        persist("OTHER", "96385074");
        em.flush();
        em.clear();
        Statistics statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertThat(skus.findBySkuCodes(java.util.List.of("NIKE-BLK-42"))).hasSize(1);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        statistics.clear();
        assertThat(products.findForScannerName("Air Max", PageRequest.of(0, 5))).hasSize(2);
    }

    private void persist(String skuCode, String barcode) {
        Product product = new Product();
        product.setName("Nike Air Max");
        product.setIsActive(true);
        product.setDeleted(false);
        em.persist(product);
        ProductSku sku = new ProductSku();
        sku.setProduct(product);
        sku.setSku(skuCode);
        sku.setBarcode(barcode);
        sku.setDescription(skuCode);
        sku.setPrice(new BigDecimal("120.00"));
        em.persist(sku);
    }
}
