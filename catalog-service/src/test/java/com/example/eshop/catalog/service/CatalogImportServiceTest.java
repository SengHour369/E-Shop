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
    @Mock BranchService branchService; @Mock BranchRepository branches; @Mock SubCategoryRepository subcategories; @Mock ProductService productService;
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
    @Test void approvedCategoryUpdateRejectsStaleVersionAndReusesTheExistingDomainService() throws Exception {
        when(permissions.current()).thenReturn(new PermissionSnapshot(2,false,Set.of("IMPORT_REVIEW","IMPORT_APPROVE","IMPORT_EXECUTE","CATEGORY_UPDATE")));
        var category = new Category();category.setId(55L);category.setVersion(3L);
        when(categories.findByCategoryId(55L)).thenReturn(Optional.of(category));
        var row = new ImportContracts.Row(ImportTool.CATEGORY_BULK_UPDATE,Map.of("id","55","expectedVersion","2","name","Updated","status","true"));
        assertThat(service.validate(row).errors()).contains("STALE_RESOURCE_VERSION");
        verifyNoInteractions(categoryService);
        var approved = new ImportContracts.Row(ImportTool.CATEGORY_BULK_UPDATE,Map.of("id","55","expectedVersion","3","name","Updated","status","true"));
        receipt(approved);
        assertThat(service.execute(key,"proof",approved).resourceId()).isEqualTo("55");
        assertThat(service.execute(key,"proof",approved).resourceId()).isEqualTo("55");
        verify(categoryService,times(1)).updateCategory(eq(55L),argThat(request -> request.getName().equals("Updated")));
        verify(proof,times(2)).verify("proof",key,approved,2);
        assertThat(ImportTool.CATEGORY_BULK_UPDATE.highRisk).isTrue();
    }
    @Test void branchAndProductUpdatesRequireAnExistingCurrentResource() {
        when(permissions.current()).thenReturn(new PermissionSnapshot(2,false,Set.of("IMPORT_REVIEW","BRANCH_UPDATE","PRODUCT_UPDATE")));
        var branch = new Branch();branch.setId(1L);branch.setVersion(5);
        when(branches.findById(1L)).thenReturn(Optional.of(branch));
        assertThat(service.validate(new ImportContracts.Row(ImportTool.BRANCH_BULK_UPDATE,Map.of("id","1","expectedVersion","4","name","Branch"))).errors()).contains("STALE_RESOURCE_VERSION");
        assertThat(service.validate(new ImportContracts.Row(ImportTool.PRODUCT_BULK_UPDATE,Map.of("id","99","expectedVersion","0","name","Product","subCategoryId","1","isActive","true"))).errors()).contains("UNKNOWN_PRODUCT");
        verifyNoInteractions(branchService,productService);
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
    ImportContracts.Row productRow()throws Exception{
        var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(4,4,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);
        var image=com.example.eshop.common.imports.ImportImageFile.sanitize(new org.springframework.mock.web.MockMultipartFile("file","product.png","image/png",out.toByteArray()));
        return new ImportContracts.Row(ImportTool.PRODUCT_BULK_CREATE,Map.of("name","Phone","subCategoryId","1","isActive","true","imageIds",UUID.randomUUID().toString(),"imageChecksums",image.checksum()));
    }
    com.example.eshop.common.imports.ImportImageFile approvedPhoto()throws Exception{
        var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(4,4,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);
        return com.example.eshop.common.imports.ImportImageFile.sanitize(new org.springframework.mock.web.MockMultipartFile("file","product.png","image/png",out.toByteArray()));
    }
    void productPermission(){when(permissions.current()).thenReturn(new PermissionSnapshot(2,false,Set.of("IMPORT_REVIEW","IMPORT_APPROVE","IMPORT_EXECUTE","PRODUCT_CREATE","BRANCH_CREATE")));}
    @Test void branchImportUsesDomainServiceAndReceipt()throws Exception{
        productPermission();var row=new ImportContracts.Row(ImportTool.BRANCH_BULK_CREATE,Map.of("name","Central","address","City"));receipt(row);var branch=new Branch();branch.setId(52L);when(branchService.create("Central","City")).thenReturn(branch);
        assertThat(service.execute(key,"proof",row).resourceId()).isEqualTo("52");assertThat(service.execute(key,"proof",row).resourceId()).isEqualTo("52");verify(branchService,times(1)).create("Central","City");
    }
    @Test void duplicateBranchAndUnknownProductCategoryFailValidation()throws Exception{
        productPermission();when(branches.existsByNameKey("central")).thenReturn(true);assertThat(service.validate(new ImportContracts.Row(ImportTool.BRANCH_BULK_CREATE,Map.of("name","Central"))).errors()).contains("DUPLICATE_BRANCH");assertThat(service.validate(productRow()).errors()).contains("MISSING_CATEGORY");
    }
    @Test void productUsesApprovedImagesAndExistingServiceExactlyOnce()throws Exception{
        productPermission();var row=productRow();receipt(row);when(subcategories.findById(1L)).thenReturn(Optional.of(new SubCategory()));when(productService.createProduct(any(),anyList(),anyList())).thenReturn(ResponseErrorTemplate.success("Created",Map.of("id",44)));
        assertThat(service.execute(key,"proof",row,List.of(approvedPhoto())).resourceId()).isEqualTo("44");assertThat(service.execute(key,"proof",row,List.of()).resourceId()).isEqualTo("44");verify(productService,times(1)).createProduct(argThat(r->r.getSubCategoryId()==1&&r.getIsActive()),argThat(files->files.size()==1&&files.get(0).getContentType().equals("image/png")),anyList());
    }
    @Test void missingOrChangedImageNeverCreatesProduct()throws Exception{
        productPermission();var row=productRow();receipt(row);when(subcategories.findById(1L)).thenReturn(Optional.of(new SubCategory()));assertThatThrownBy(()->service.execute(key,"proof",row,List.of())).hasMessageContaining("attachments required");assertThatThrownBy(()->service.execute(key,"proof",row,List.of(new com.example.eshop.common.imports.ImportImageFile("bad.png",new byte[]{1,2,3})))).hasMessageContaining("differs from approved");verifyNoInteractions(productService);
    }
    @Test void branchDeniedInventoryCannotPassValidation(){
        var item=new Inventory();item.setId(1L);item.setBranchId(3L);item.setReservedQuantity(0L);when(inventory.findById(1L)).thenReturn(Optional.of(item));doThrow(new org.springframework.security.access.AccessDeniedException("Denied")).when(branchService).scope(eq(3L),any());
        assertThat(service.validate(new ImportContracts.Row(ImportTool.INVENTORY_STOCK_IN_BULK,Map.of("inventoryId","1","quantity","5"))).errors()).contains("BRANCH_ACCESS_DENIED");
    }
}
