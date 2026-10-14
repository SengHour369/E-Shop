package com.example.eshop.catalog.service;

import com.example.eshop.catalog.controller.AiCatalogController;
import com.example.eshop.catalog.repository.*;
import com.example.eshop.catalog.model.*;
import com.example.eshop.catalog.dto.request.*;
import com.example.eshop.catalog.dto.response.ResponseErrorTemplate;
import com.example.eshop.catalog.service.impl.ProductSkuServiceImpl;
import com.example.eshop.catalog.util.SkuGeneratorUtil;
import com.example.eshop.common.imports.*;
import com.example.eshop.common.security.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.security.MessageDigest;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CatalogImportServiceTest {
    @Mock LivePermissionService permissions;
    @Mock CategoryRepository categories;
    @Mock ProductRepository products;
    @Mock ProductSkuRepository skus;
    @Mock InventoryRepository inventory;
    @Mock CategoryService categoryService;
    @Mock ProductSkuServiceImpl skuService;
    @Mock InventoryService inventoryService;
    @Mock AiCatalogController promotions;
    @Mock SkuGeneratorUtil skuGenerator;
    @Spy ObjectMapper mapper = new ObjectMapper();
    @Mock JdbcTemplate jdbc;
    @Mock EntityManager em;
    @Mock ImportApprovalProof proof;
    @Mock PromotionRepository promotionRepository;
    @InjectMocks CatalogImportService service;
    String key = UUID.randomUUID() + ":" + UUID.randomUUID();
    @BeforeEach void setup() {
        var actor = new UsernamePasswordAuthenticationToken("reviewer",null,List.of()); actor.setDetails(Map.of("userId",2L)); SecurityContextHolder.getContext().setAuthentication(actor);
        when(permissions.current()).thenReturn(new PermissionSnapshot(2,false,Set.of("IMPORT_REVIEW","IMPORT_APPROVE","IMPORT_EXECUTE","CATEGORY_CREATE","INVENTORY_ADJUST","SKU_CREATE")));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    ImportReceipt receipt(ImportContracts.Row row) throws Exception {
        var receipt = new ImportReceipt(); receipt.setId(key); receipt.setActorId(2);
        receipt.setChecksum(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(List.of(row.tool().name(),new TreeMap<>(row.values()))))));
        when(em.find(ImportReceipt.class,key,LockModeType.PESSIMISTIC_WRITE)).thenReturn(receipt); return receipt;
    }
    @Test void receiptReplayDoesNotCreateAgainAndRejectsPayloadChanges() throws Exception {
        var row = new ImportContracts.Row(ImportTool.CATEGORY_BULK_CREATE,Map.of("name","New","status","true")); receipt(row);
        when(categoryService.createCategory(any())).thenReturn(ResponseErrorTemplate.success("Created",Map.of("id",55)));
        assertThat(service.execute(key,"proof",row).resourceId()).isEqualTo("55");
        assertThat(service.execute(key,"proof",row).resourceId()).isEqualTo("55");
        verify(categoryService,times(1)).createCategory(any()); verify(proof,times(2)).verify("proof",key,row,2);
        assertThatThrownBy(() -> service.execute(key,"proof",new ImportContracts.Row(row.tool(),Map.of("name","Changed","status","true")))).hasMessageContaining("mismatch");
    }
    @Test void stockInLocksInventoryAndAddsToCurrentQuantity() throws Exception {
        var row = new ImportContracts.Row(ImportTool.INVENTORY_STOCK_IN_BULK,Map.of("inventoryId","1","quantity","5")); receipt(row);
        var item = new Inventory(); item.setId(1L); item.setQuantity(20L); item.setReservedQuantity(10L);
        when(inventory.findById(1L)).thenReturn(Optional.of(item)); when(inventory.lockById(1L)).thenReturn(Optional.of(item));
        assertThat(service.execute(key,"proof",row).resourceId()).isEqualTo("1");
        verify(inventoryService).restock(eq(1L),argThat(request -> request.getQuantity() == 5L));
    }
    @Test void reservedStockAndUnknownProductFailDeterministicValidation() {
        var item = new Inventory(); item.setReservedQuantity(10L); when(inventory.findById(1L)).thenReturn(Optional.of(item));
        assertThat(service.validate(new ImportContracts.Row(ImportTool.INVENTORY_STOCK_ADJUST_BULK,Map.of("inventoryId","1","quantity","5"))).errors()).contains("BELOW_RESERVED_STOCK");
        assertThat(service.validate(new ImportContracts.Row(ImportTool.PRODUCT_SKU_BULK_CREATE,Map.of("productId","99","description","SKU","price","1","quantity","0"))).errors()).contains("UNKNOWN_PRODUCT");
        verifyNoInteractions(inventoryService,skuService);
    }
    @Test void requiresExplicitInventoryAuthorizationAndApprovalProof() {
        var row = new ImportContracts.Row(ImportTool.INVENTORY_STOCK_ADJUST_BULK,Map.of("inventoryId","1","quantity","5"));
        when(permissions.current()).thenReturn(new PermissionSnapshot(2,false,Set.of("IMPORT_REVIEW","IMPORT_EXECUTE")));
        assertThatThrownBy(() -> service.execute(key,"proof",row)).hasMessageContaining("permission"); verifyNoInteractions(inventoryService,em);
    }
    @Test void bulkValidationAuthorizesOncePerToolAndEnforcesItsLimit() {
        var row = new ImportContracts.Row(ImportTool.CATEGORY_BULK_CREATE,Map.of("name","New","status","true"));
        assertThat(service.validateBulk(Collections.nCopies(500,row))).hasSize(500);
        verify(permissions,times(1)).current();
        assertThatThrownBy(() -> service.validateBulk(Collections.nCopies(501,row))).hasMessageContaining("500");
    }
}
