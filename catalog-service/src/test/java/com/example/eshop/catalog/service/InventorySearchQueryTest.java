package com.example.eshop.catalog.service;

import com.example.eshop.catalog.model.Inventory;
import com.example.eshop.catalog.model.Product;
import com.example.eshop.catalog.model.ProductSku;
import com.example.eshop.catalog.repository.InventoryRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:inventory;MODE=PostgreSQL;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = InventorySearchQueryTest.Config.class)
class InventorySearchQueryTest {
    @Configuration
    @EntityScan("com.example.eshop.catalog.model")
    @EnableJpaRepositories("com.example.eshop.catalog.repository")
    static class Config {}

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry properties) {
        String url = System.getenv("ESHOP_INVENTORY_TEST_POSTGRES_URL");
        if (url == null) return;
        if (!url.endsWith("/inventory_verification")) {
            throw new IllegalArgumentException("A disposable inventory_verification database is required");
        }
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> "postgres");
        properties.add("spring.datasource.password", () -> System.getenv("ESHOP_TEST_POSTGRES_PASSWORD"));
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired EntityManager em;
    @Autowired InventoryRepository inventories;

    @BeforeEach
    void seed() {
        seed("Wireless Mouse", "MOUSE-001", "AbC123", "A", 20L);
        seed("Keyboard", "KEY-001", "XyZ789", "B", 2L);
        em.flush();
        em.clear();
    }

    @Test
    void omittedFiltersReturnAllInventoryAndCountAcrossPages() {
        var page = inventories.searchInventory(null, null, null, PageRequest.of(0, 1));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void emptySearchReturnsAllInventory() {
        assertThat(inventories.searchInventory("", null, null, PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(2);
    }

    @Test
    void searchMatchesProductSkuAndBarcodeIgnoringCase() {
        for (String search : new String[]{"wireless", "mouse-001", "abc123"}) {
            var page = inventories.searchInventory(search, null, null, PageRequest.of(0, 10));
            assertThat(page.getContent()).extracting(i -> i.getProductSku().getSku())
                    .containsExactly("MOUSE-001");
        }
    }

    @Test
    void warehouseAndStatusWorkWithoutSearch() {
        var page = inventories.searchInventory(null, "B", "LOW_STOCK", PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(i -> i.getProductSku().getSku()).containsExactly("KEY-001");
        assertThat(inventories.searchInventory(null, "B", "IN_STOCK", PageRequest.of(0, 10)).isEmpty())
                .isTrue();
    }

    private void seed(String name, String skuCode, String barcode, String warehouse, long quantity) {
        Product product = new Product();
        product.setName(name);
        em.persist(product);
        ProductSku sku = new ProductSku();
        sku.setProduct(product);
        sku.setSku(skuCode);
        sku.setBarcode(barcode);
        sku.setDescription(name);
        sku.setPrice(BigDecimal.TEN);
        em.persist(sku);
        Inventory inventory = new Inventory();
        inventory.setProductSku(sku);
        inventory.setQuantity(quantity);
        inventory.setWarehouseLocation(warehouse);
        em.persist(inventory);
    }
}
