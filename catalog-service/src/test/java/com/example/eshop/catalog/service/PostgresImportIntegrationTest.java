package com.example.eshop.catalog.service;

import com.example.eshop.catalog.controller.AiCatalogController;
import com.example.eshop.catalog.model.*;
import com.example.eshop.catalog.repository.*;
import com.example.eshop.catalog.service.impl.*;
import com.example.eshop.catalog.util.SkuGeneratorUtil;
import com.example.eshop.common.imports.*;
import com.example.eshop.common.security.*;
import com.example.eshop.common.audit.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.*;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "ESHOP_IMPORT_TEST_POSTGRES_URL", matches = ".+/import_verification")
@DataJpaTest(showSql = false, properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "ai.import.signing-key=test-key-with-at-least-thirty-two-characters"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PostgresImportIntegrationTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PostgresImportIntegrationTest {
    @Configuration
    @org.springframework.boot.autoconfigure.domain.EntityScan({"com.example.eshop.catalog.model", "com.example.eshop.common.audit"})
    @org.springframework.data.jpa.repository.config.EnableJpaRepositories({"com.example.eshop.catalog.repository", "com.example.eshop.common.audit"})
    @Import({CatalogImportService.class, InventoryServiceImpl.class, ImportApprovalProof.class})
    static class Config {
        @Bean ObjectMapper mapper() { return new ObjectMapper(); }
        @Bean JdbcTemplate jdbc(javax.sql.DataSource ds) { return new JdbcTemplate(ds); }
        @Bean TransactionTemplate tx(PlatformTransactionManager manager) { return new TransactionTemplate(manager); }
    }
    @DynamicPropertySource static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("ESHOP_IMPORT_TEST_POSTGRES_URL"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "import-test-only");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }
    @Autowired CatalogImportService service;
    @Autowired ImportApprovalProof proof;
    @Autowired EntityManager em;
    @Autowired TransactionTemplate tx;
    @Autowired InventoryRepository inventories;
    @Autowired StockMovementRepository movements;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean LivePermissionService permissions;
    @MockitoBean AuditLogService audit;
    @MockitoBean BranchService branchService;
    @MockitoBean ProductService productService;
    @MockitoBean CategoryService categories;
    @MockitoBean ProductSkuServiceImpl skus;
    @MockitoBean AiCatalogController promotions;
    @MockitoBean SkuGeneratorUtil generator;
    @BeforeEach void setup() {
        actor(); when(permissions.current()).thenReturn(new PermissionSnapshot(2,false,Set.of("IMPORT_UPLOAD","IMPORT_REVIEW","IMPORT_APPROVE","IMPORT_EXECUTE","INVENTORY_ADJUST","CATEGORY_CREATE")));
    }
    void actor() { var actor = new UsernamePasswordAuthenticationToken("reviewer",null,List.of()); actor.setDetails(Map.of("userId",2L)); SecurityContextHolder.getContext().setAuthentication(actor); }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    Long inventory() {
        return tx.execute(t -> {
            var product = new Product(); product.setName("Test"); product.setIsActive(true); em.persist(product);
            var sku = new ProductSku(); sku.setProduct(product); sku.setSku(UUID.randomUUID().toString()); sku.setDescription("Test"); sku.setPrice(BigDecimal.ONE); em.persist(sku);
            var inventory = new Inventory(); inventory.setProductSku(sku); inventory.setQuantity(20L); inventory.setReservedQuantity(10L); inventory.setAvailableQuantity(10L); em.persist(inventory); return inventory.getId();
        });
    }
    @Test void concurrentDeliveryCommitsExactlyOneStockMovement() throws Exception {
        long inventoryId = inventory(); String key = UUID.randomUUID()+":"+UUID.randomUUID();
        var row = new ImportContracts.Row(ImportTool.INVENTORY_STOCK_IN_BULK,Map.of("inventoryId",Long.toString(inventoryId),"quantity","5"));
        String signed = proof.issue(key,row,2,1); var start = new CountDownLatch(1); var pool = Executors.newFixedThreadPool(2);
        try {
            Callable<String> call = () -> { actor(); start.await(); try { return service.execute(key,signed,row).resourceId(); } finally { clear(); } };
            var first = pool.submit(call); var second = pool.submit(call); start.countDown();
            assertThat(first.get(20,TimeUnit.SECONDS)).isEqualTo(Long.toString(inventoryId));
            assertThat(second.get(20,TimeUnit.SECONDS)).isEqualTo(Long.toString(inventoryId));
            assertThat(inventories.findById(inventoryId).orElseThrow().getQuantity()).isEqualTo(25);
            assertThat(inventories.findById(inventoryId).orElseThrow().getAvailableQuantity()).isEqualTo(15);
            assertThat(movements.findByInventoryIdOrderByCreatedAtDesc(inventoryId,org.springframework.data.domain.PageRequest.of(0,10)).getTotalElements()).isEqualTo(1);
            assertThat(movements.findByInventoryIdOrderByCreatedAtDesc(inventoryId,org.springframework.data.domain.PageRequest.of(0,10)).getContent().get(0).getMovementType()).isEqualTo("RESTOCK");
            actor(); assertThat(service.execute(key,signed,row).resourceId()).isEqualTo(Long.toString(inventoryId));
        } finally { pool.shutdownNow(); }
    }
    @Test void invalidAdjustmentRollsBackReceiptAndStock() {
        long inventoryId = inventory(); String key = UUID.randomUUID()+":"+UUID.randomUUID();
        var row = new ImportContracts.Row(ImportTool.INVENTORY_STOCK_ADJUST_BULK,Map.of("inventoryId",Long.toString(inventoryId),"quantity","5"));
        assertThatThrownBy(() -> service.execute(key,proof.issue(key,row,2,1),row)).hasMessageContaining("BELOW_RESERVED_STOCK");
        assertThat(inventories.findById(inventoryId).orElseThrow().getQuantity()).isEqualTo(20);
        assertThat(jdbc.queryForObject("select count(*) from ai_import_receipt where id=?",Integer.class,key)).isZero();
    }
    @Test void naturalKeyLockAndCategoryReceiptWorkOnPostgres() throws Exception {
        String key = UUID.randomUUID()+":"+UUID.randomUUID(); var row = new ImportContracts.Row(ImportTool.CATEGORY_BULK_CREATE,Map.of("name","New","status","true"));
        when(categories.createCategory(any())).thenReturn(com.example.eshop.catalog.dto.response.ResponseErrorTemplate.success("Created",Map.of("id",99)));
        assertThat(service.execute(key,proof.issue(key,row,2,1),row).resourceId()).isEqualTo("99");
        assertThat(service.execute(key,proof.issue(key,row,2,1),row).resourceId()).isEqualTo("99");
        verify(categories,times(1)).createCategory(any());
    }
}
