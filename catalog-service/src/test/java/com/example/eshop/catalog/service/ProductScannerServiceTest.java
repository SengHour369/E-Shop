package com.example.eshop.catalog.service;

import com.example.eshop.catalog.dto.request.ScanRequest;
import com.example.eshop.catalog.dto.response.PriceResult;
import com.example.eshop.catalog.enumeration.PromotionType;
import com.example.eshop.catalog.model.*;
import com.example.eshop.catalog.repository.*;
import com.example.eshop.catalog.scanner.*;
import com.example.eshop.catalog.service.impl.ProductScannerService;
import com.example.eshop.catalog.service.impl.PromotionPricingService;
import com.example.eshop.common.exception.BusinessLogicException;
import com.example.eshop.common.request.RequestIds;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductScannerServiceTest {
    private final ProductSkuRepository skus = mock(ProductSkuRepository.class);
    private final InventoryRepository inventories = mock(InventoryRepository.class);
    private final ProductAttributeRepository attributes = mock(ProductAttributeRepository.class);
    private final VariantAttributeRepository variantAttributes = mock(VariantAttributeRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final PromotionPricingService pricing = mock(PromotionPricingService.class);
    private final ProductVisionClient vision = mock(ProductVisionClient.class);
    private final AtomicLong clock = new AtomicLong(10_000);
    private ProductScannerService scanner;

    @BeforeEach void setUp() {
        scanner = new ProductScannerService(skus, inventories, attributes, variantAttributes, products, pricing, vision,
                new ScanCoalescer(1500, clock::get), new ScanRateLimiter(30, 10, clock::get));
        signIn("USER");
        MDC.put("requestId", "req_scan_1");
        when(attributes.findWithValuesForSkus(any())).thenReturn(List.of());
        when(variantAttributes.findDetailedByProductSkuId(any())).thenReturn(List.of());
    }

    @AfterEach void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test void eanFindsSkuPricePromotionStockAndVariantsWithoutChangingPrice() {
        ProductSku sku = sku("NIKE-BLK-42", "8850123456787", "120.00", true);
        Inventory inventory = inventory(20, 6, "A-1");
        when(skus.findByBarcodes(any())).thenReturn(List.of(sku));
        when(inventories.findByProductSkuId(501L)).thenReturn(Optional.of(inventory));
        when(pricing.calculatePrices(any())).thenReturn(java.util.Map.of(501L, price(true)));
        ProductAttribute color = attribute(1L, "Color");
        ProductAttributeValue black = new ProductAttributeValue();
        black.setValue("Black");
        when(attributes.findWithValuesForSkus(any())).thenReturn(List.of(row(color, black)));
        VariantAttribute size = new VariantAttribute();
        size.setAttribute(attribute(2L, "Size"));
        ProductAttributeValue fortyTwo = new ProductAttributeValue();
        fortyTwo.setValue("42");
        size.setAttributeValue(fortyTwo);
        when(variantAttributes.findDetailedByProductSkuId(501L)).thenReturn(List.of(size));

        var response = scanner.scan(new ScanRequest("8850123456787", ScanFormat.EAN_13));

        assertThat(response.isFound()).isTrue();
        assertThat(response.getSku()).isEqualTo("NIKE-BLK-42");
        assertThat(response.getBarcode()).isEqualTo("8850123456787");
        assertThat(response.getName()).isEqualTo("Nike Air Max");
        assertThat(response.getOriginalPrice()).isEqualByComparingTo("120.00");
        assertThat(response.getFinalPrice()).isEqualByComparingTo("96.00");
        assertThat(response.getHasPromotion()).isTrue();
        assertThat(response.getDiscountPercentage()).isEqualByComparingTo("20.00");
        assertThat(response.getAvailableQuantity()).isEqualTo(14);
        assertThat(response.getQuantity()).isEqualTo(20);
        assertThat(response.getReservedQuantity()).isEqualTo(6);
        assertThat(response.getInStock()).isTrue();
        assertThat(response.getVariant()).extracting(item -> item.name() + "=" + item.value())
                .contains("Size=42", "Color=Black");
        assertThat(response.getRequestId()).isEqualTo("req_scan_1");
        assertThat(response.getWarehouseLocation()).isNull();
        assertThat(sku.getPrice()).isEqualByComparingTo("120.00");
        verify(skus, never()).findBySkuCodes(any());
    }

    @Test void upcQrAndInternalSkuUseIndexedLookups() {
        ProductSku sku = sku("NIKE-BLK-42", "036000291452", "120.00", true);
        when(skus.findByBarcodes(any())).thenReturn(List.of(sku));
        when(inventories.findByProductSkuId(501L)).thenReturn(Optional.empty());
        when(pricing.calculatePrices(any())).thenReturn(java.util.Map.of(501L, price(false)));
        assertThat(scanner.scan(new ScanRequest("036000291452", ScanFormat.UPC_A)).getBarcode()).isEqualTo("036000291452");

        when(skus.findByBarcodes(any())).thenReturn(List.of());
        when(skus.findBySkuCodes(any())).thenReturn(List.of(sku));
        assertThat(scanner.scan(new ScanRequest("NIKE-BLK-42", ScanFormat.QR_CODE)).getSku()).isEqualTo("NIKE-BLK-42");
        assertThat(scanner.scan(new ScanRequest("NIKE-BLK-42", ScanFormat.SKU)).isFound()).isTrue();
    }

    @Test void unknownBarcodeIsANormalMiss() {
        when(skus.findByBarcodes(any())).thenReturn(List.of());
        var response = scanner.scan(new ScanRequest("96385074", ScanFormat.EAN_8));
        assertThat(response.isFound()).isFalse();
        assertThat(response.getCode()).isEqualTo("96385074");
        assertThat(response.getMessage()).isEqualTo("Product not found");
        assertThat(response.getProductId()).isNull();
    }

    @Test void inactiveProductStaysVisibleButIsNotSellable() {
        ProductSku sku = sku("NIKE-BLK-42", "8850123456787", "120.00", false);
        when(skus.findByBarcodes(any())).thenReturn(List.of(sku));
        when(inventories.findByProductSkuId(501L)).thenReturn(Optional.of(inventory(10, 0, "A-1")));
        when(pricing.calculatePrices(any())).thenReturn(java.util.Map.of(501L, price(false)));
        var response = scanner.scan(new ScanRequest("8850123456787", ScanFormat.EAN_13));
        assertThat(response.isFound()).isTrue();
        assertThat(response.getActive()).isFalse();
        assertThat(response.getAvailableForSale()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Product is not available");
    }

    @Test void outOfStockIsFoundAndNotSellable() {
        ProductSku sku = sku("NIKE-BLK-42", "8850123456787", "120.00", true);
        when(skus.findByBarcodes(any())).thenReturn(List.of(sku));
        when(inventories.findByProductSkuId(501L)).thenReturn(Optional.of(inventory(4, 4, "A-1")));
        when(pricing.calculatePrices(any())).thenReturn(java.util.Map.of(501L, price(false)));
        var response = scanner.scan(new ScanRequest("8850123456787", ScanFormat.EAN_13));
        assertThat(response.isFound()).isTrue();
        assertThat(response.getInStock()).isFalse();
        assertThat(response.getAvailableQuantity()).isZero();
        assertThat(response.getLowStock()).isFalse();
        assertThat(response.getAvailableForSale()).isFalse();
    }

    @Test void productWithoutPromotionKeepsTheSkuPrice() {
        ProductSku sku = sku("NIKE-BLK-42", "8850123456787", "100.00", true);
        when(skus.findByBarcodes(any())).thenReturn(List.of(sku));
        when(inventories.findByProductSkuId(501L)).thenReturn(Optional.of(inventory(3, 0, "A-1")));
        when(pricing.calculatePrices(any())).thenReturn(java.util.Map.of(501L, price(false)));
        var response = scanner.scan(new ScanRequest("8850123456787", ScanFormat.EAN_13));
        assertThat(response.getHasPromotion()).isFalse();
        assertThat(response.getOriginalPrice()).isEqualByComparingTo("100.00");
        assertThat(response.getFinalPrice()).isEqualByComparingTo("100.00");
        assertThat(response.getLowStock()).isTrue();
        assertThat(sku.getPrice()).isEqualByComparingTo("100.00");
    }

    @Test void repeatedCodeWithinTheWindowLoadsOnce() {
        ProductSku sku = sku("NIKE-BLK-42", "8850123456787", "120.00", true);
        when(skus.findByBarcodes(any())).thenReturn(List.of(sku));
        when(inventories.findByProductSkuId(501L)).thenReturn(Optional.of(inventory(20, 6, "A-1")));
        when(pricing.calculatePrices(any())).thenReturn(java.util.Map.of(501L, price(true)));
        scanner.scan(new ScanRequest("8850123456787", ScanFormat.EAN_13));
        scanner.scan(new ScanRequest("8850123456787", ScanFormat.EAN_13));
        verify(skus, times(1)).findByBarcodes(any());
        verify(pricing, times(1)).calculatePrices(any());
    }

    @Test void invalidCodeDoesNotTouchTheDatabase() {
        assertThatThrownBy(() -> scanner.scan(new ScanRequest("8850123456789", ScanFormat.EAN_13)))
                .isInstanceOf(BusinessLogicException.class);
        verifyNoInteractions(skus, inventories, pricing);
    }

    @Test void distinctScansAreRateLimited() {
        ProductScannerService limited = new ProductScannerService(skus, inventories, attributes, variantAttributes,
                products, pricing, vision, new ScanCoalescer(1500, clock::get), new ScanRateLimiter(2, 10, clock::get));
        when(skus.findByBarcodes(any())).thenReturn(List.of());
        limited.scan(new ScanRequest("8850123456787", ScanFormat.EAN_13));
        limited.scan(new ScanRequest("96385074", ScanFormat.EAN_8));
        assertThatThrownBy(() -> limited.scan(new ScanRequest("036000291452", ScanFormat.UPC_A)))
                .isInstanceOf(ScanRateLimitedException.class);
    }

    @Test void staffCanSeeWarehouseAndCustomersCannot() {
        ProductSku sku = sku("NIKE-BLK-42", "8850123456787", "120.00", true);
        when(skus.findByBarcodes(any())).thenReturn(List.of(sku));
        when(inventories.findByProductSkuId(501L)).thenReturn(Optional.of(inventory(20, 6, "A-1")));
        when(pricing.calculatePrices(any())).thenReturn(java.util.Map.of(501L, price(false)));
        signIn("STAFF");
        assertThat(scanner.scan(new ScanRequest("8850123456787", ScanFormat.EAN_13)).getWarehouseLocation()).isEqualTo("A-1");
        clock.addAndGet(2_000);
        signIn("USER");
        assertThat(scanner.scan(new ScanRequest("8850123456787", ScanFormat.EAN_13)).getWarehouseLocation()).isNull();
    }

    @Test void imageRecognitionUsesCatalogMatchesAndFailsClosed() {
        Product product = new Product();
        product.setId(100L);
        product.setName("Nike Air Max");
        product.setDeleted(false);
        when(vision.identify(any(), any())).thenReturn(new VisionOutcome(true, List.of(new VisionLabel("Nike Air Max", 0.95))));
        when(products.findForScannerName(any(), any())).thenReturn(List.of(product));
        when(products.findImageUrls(any())).thenReturn(List.of());
        when(skus.findForScannerProducts(any())).thenReturn(List.of());
        var matched = scanner.recognize(jpeg());
        assertThat(matched.getMatchType()).isEqualTo("AI_IMAGE");
        assertThat(matched.getCandidates()).extracting(item -> item.getProductId()).containsExactly(100L);

        when(vision.identify(any(), any())).thenReturn(VisionOutcome.unavailable());
        var down = scanner.recognize(jpeg());
        assertThat(down.getMatchType()).isEqualTo("AI_UNAVAILABLE");
        assertThat(down.getCandidates()).isEmpty();
    }

    private void signIn(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "scanner-user", null, List.of(new SimpleGrantedAuthority(role))));
    }

    private static ProductSku sku(String code, String barcode, String price, boolean active) {
        Product product = new Product();
        product.setId(100L);
        product.setName("Nike Air Max");
        product.setIsActive(active);
        product.setDeleted(false);
        ProductSku sku = new ProductSku();
        sku.setId(501L);
        sku.setSku(code);
        sku.setBarcode(barcode);
        sku.setPrice(new BigDecimal(price));
        sku.setProduct(product);
        Image image = new Image();
        image.setUrl("https://cdn.example/nike.jpg");
        sku.setImage(image);
        return sku;
    }

    private static Inventory inventory(long quantity, long reserved, String warehouse) {
        return Inventory.builder().quantity(quantity).reservedQuantity(reserved)
                .availableQuantity(quantity - reserved).lowStockThreshold(5).warehouseLocation(warehouse).build();
    }

    private static PriceResult price(boolean promotion) {
        return new PriceResult(501L, new BigDecimal(promotion ? "120.00" : "100.00"),
                new BigDecimal(promotion ? "96.00" : "100.00"),
                new BigDecimal(promotion ? "24.00" : "0.00"),
                new BigDecimal(promotion ? "20.00" : "0.00"),
                promotion ? 9L : null, promotion ? "Sale" : null, promotion ? PromotionType.FLASH_SALE : null,
                promotion, null, null);
    }

    private static ProductAttribute attribute(long id, String name) {
        ProductAttribute attribute = new ProductAttribute();
        attribute.setId(id);
        attribute.setName(name);
        return attribute;
    }

    private static ProductAttributeRepository.AttributeValue row(ProductAttribute attribute, ProductAttributeValue value) {
        return new ProductAttributeRepository.AttributeValue() {
            public ProductAttribute getAttribute() { return attribute; }
            public ProductAttributeValue getAttributeValue() { return value; }
        };
    }

    private static MockMultipartFile jpeg() {
        return new MockMultipartFile("image", "scan.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00});
    }
}
