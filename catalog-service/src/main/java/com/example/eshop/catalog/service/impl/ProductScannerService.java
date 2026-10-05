package com.example.eshop.catalog.service.impl;

import com.example.eshop.catalog.dto.request.ScanRequest;
import com.example.eshop.catalog.dto.response.ImageMatchResponse;
import com.example.eshop.catalog.dto.response.PriceResult;
import com.example.eshop.catalog.dto.response.ScanResponse;
import com.example.eshop.catalog.dto.response.ScanVariant;
import com.example.eshop.catalog.model.Inventory;
import com.example.eshop.catalog.model.Product;
import com.example.eshop.catalog.model.ProductAttribute;
import com.example.eshop.catalog.model.ProductSku;
import com.example.eshop.catalog.model.VariantAttribute;
import com.example.eshop.catalog.repository.InventoryRepository;
import com.example.eshop.catalog.repository.ProductAttributeRepository;
import com.example.eshop.catalog.repository.ProductRepository;
import com.example.eshop.catalog.repository.ProductSkuRepository;
import com.example.eshop.catalog.repository.VariantAttributeRepository;
import com.example.eshop.catalog.scanner.CatalogImageMatcher;
import com.example.eshop.catalog.scanner.ProductVisionClient;
import com.example.eshop.catalog.scanner.ScanCoalescer;
import com.example.eshop.catalog.scanner.ScanCodes;
import com.example.eshop.catalog.scanner.ScanRateLimiter;
import com.example.eshop.catalog.scanner.VisionOutcome;
import com.example.eshop.common.exception.BusinessLogicException;
import com.example.eshop.common.request.RequestIds;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductScannerService {
    private static final Logger log = LoggerFactory.getLogger(ProductScannerService.class);
    private static final long MAX_IMAGE_BYTES = 1_500_000;
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> OPERATIONAL = Set.of("ADMIN", "MANAGER", "STAFF");

    private final ProductSkuRepository skus;
    private final InventoryRepository inventories;
    private final ProductAttributeRepository attributes;
    private final VariantAttributeRepository variantAttributes;
    private final ProductRepository products;
    private final PromotionPricingService pricing;
    private final ProductVisionClient vision;
    private final ScanCoalescer coalescer;
    private final ScanRateLimiter limiter;

    @Transactional(readOnly = true)
    public ScanResponse scan(ScanRequest request) {
        long started = System.nanoTime();
        String format = request.format() == null ? null : request.format().name();
        boolean found = false;
        try {
            ScanCodes.Normalized normalized = ScanCodes.normalize(request.code(), request.format());
            format = normalized.format().name();
            String actor = actorKey();
            LoadedScan loaded = coalescer.get(actor + "\n" + normalized.format() + "\n" + normalized.code(), () -> {
                limiter.acquire(actor);
                return load(normalized);
            });
            found = loaded.sku != null;
            return toResponse(normalized, loaded);
        } finally {
            log.info("product scan requestId={} actor={} format={} found={} durationMs={}",
                    RequestIds.current(), actorKey(), format, found, elapsed(started));
        }
    }

    @Transactional(readOnly = true)
    public ImageMatchResponse recognize(MultipartFile image) {
        long started = System.nanoTime();
        String matchType = "AI_UNAVAILABLE";
        int candidates = 0;
        try {
            limiter.acquire(actorKey());
            CheckedImage checked = checked(image);
            VisionOutcome outcome = vision.identify(checked.bytes(), checked.mediaType());
            ImageMatchResponse response = outcome.available()
                    ? CatalogImageMatcher.match(outcome.labels(), this::searchNames, RequestIds.current())
                    : CatalogImageMatcher.unavailable(RequestIds.current());
            matchType = response.getMatchType();
            candidates = response.getCandidates().size();
            return response;
        } finally {
            log.info("product image scan requestId={} actor={} matchType={} candidates={} durationMs={}",
                    RequestIds.current(), actorKey(), matchType, candidates, elapsed(started));
        }
    }

    private LoadedScan load(ScanCodes.Normalized code) {
        ProductSku sku = find(code);
        if (sku == null) {
            return new LoadedScan(null, null, List.of(), List.of(), null, null);
        }
        Inventory inventory = inventories.findByProductSkuId(sku.getId()).orElse(null);
        List<ProductAttributeRepository.AttributeValue> attributeRows =
                attributes.findWithValuesForSkus(List.of(sku.getId()));
        List<VariantAttribute> variants = variantAttributes.findDetailedByProductSkuId(sku.getId());
        PriceResult price = pricing.calculatePrices(List.of(sku)).get(sku.getId());
        return new LoadedScan(sku, inventory, attributeRows, variants, price, imageUrl(sku));
    }

    private ProductSku find(ScanCodes.Normalized code) {
        ProductSku match = null;
        if (!code.barcodeCandidates().isEmpty()) {
            match = prefer(skus.findByBarcodes(code.barcodeCandidates()), code);
        }
        if (match != null || code.skuCandidates().isEmpty()) {
            return match;
        }
        return prefer(skus.findBySkuCodes(code.skuCandidates()), code);
    }

    private ProductSku prefer(List<ProductSku> rows, ScanCodes.Normalized code) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        for (ProductSku sku : rows) {
            if (code.code().equals(sku.getBarcode()) || code.code().equalsIgnoreCase(sku.getSku())) {
                return sku;
            }
        }
        for (String candidate : code.barcodeCandidates()) {
            for (ProductSku sku : rows) {
                if (candidate.equals(sku.getBarcode())) {
                    return sku;
                }
            }
        }
        for (String candidate : code.skuCandidates()) {
            for (ProductSku sku : rows) {
                if (candidate.equalsIgnoreCase(sku.getSku())) {
                    return sku;
                }
            }
        }
        return rows.get(0);
    }

    private ScanResponse toResponse(ScanCodes.Normalized code, LoadedScan loaded) {
        String requestId = RequestIds.current();
        if (loaded.sku == null) {
            return ScanResponse.builder()
                    .found(false)
                    .code(code.code())
                    .format(code.format().name())
                    .message("Product not found")
                    .requestId(requestId)
                    .build();
        }
        ProductSku sku = loaded.sku;
        Product product = sku.getProduct();
        boolean active = Boolean.TRUE.equals(product.getIsActive()) && !Boolean.TRUE.equals(product.getDeleted());
        long quantity = loaded.inventory == null || loaded.inventory.getQuantity() == null ? 0 : loaded.inventory.getQuantity();
        long reserved = loaded.inventory == null || loaded.inventory.getReservedQuantity() == null ? 0 : loaded.inventory.getReservedQuantity();
        long available = Math.max(0, quantity - reserved);
        int threshold = loaded.inventory == null || loaded.inventory.getLowStockThreshold() == null
                ? 5 : loaded.inventory.getLowStockThreshold();
        boolean inStock = available > 0;
        boolean lowStock = inStock && available <= threshold;
        BigDecimal original = loaded.price == null ? PromotionPricingService.money(sku.getPrice()) : loaded.price.originalPrice();
        BigDecimal finalPrice = loaded.price == null ? original : loaded.price.finalPrice();
        boolean hasPromotion = loaded.price != null && loaded.price.promotionApplied();
        BigDecimal discount = loaded.price == null ? PromotionPricingService.money(BigDecimal.ZERO) : loaded.price.discountPercentage();
        String message = !active ? "Product is not available" : !inStock ? "Out of stock" : "Product found";
        return ScanResponse.builder()
                .found(true)
                .code(code.code())
                .format(code.format().name())
                .matchType(code.format().name())
                .message(message)
                .requestId(requestId)
                .productId(product.getId())
                .productSkuId(sku.getId())
                .sku(sku.getSku())
                .barcode(sku.getBarcode())
                .name(product.getName())
                .originalPrice(original)
                .finalPrice(finalPrice)
                .hasPromotion(hasPromotion)
                .discountPercentage(discount)
                .quantity(quantity)
                .reservedQuantity(reserved)
                .availableQuantity(available)
                .inStock(inStock)
                .lowStock(lowStock)
                .active(active)
                .availableForSale(active && inStock)
                .imageUrl(loaded.imageUrl)
                .warehouseLocation(operationalViewer() && loaded.inventory != null ? loaded.inventory.getWarehouseLocation() : null)
                .variant(variants(loaded))
                .build();
    }

    private List<ScanVariant> variants(LoadedScan loaded) {
        List<ScanVariant> result = new ArrayList<>();
        java.util.Set<String> named = new java.util.LinkedHashSet<>();
        for (VariantAttribute row : loaded.variantRows) {
            if (row.getAttribute() == null || row.getAttributeValue() == null) {
                continue;
            }
            result.add(new ScanVariant(row.getAttribute().getName(), row.getAttributeValue().getValue()));
            named.add(row.getAttribute().getName());
        }
        Map<Long, ProductAttribute> byId = new LinkedHashMap<>();
        Map<Long, List<String>> values = new LinkedHashMap<>();
        for (ProductAttributeRepository.AttributeValue row : loaded.attributeRows) {
            ProductAttribute attribute = row.getAttribute();
            if (attribute == null) {
                continue;
            }
            byId.putIfAbsent(attribute.getId(), attribute);
            if (row.getAttributeValue() != null && row.getAttributeValue().getValue() != null) {
                values.computeIfAbsent(attribute.getId(), key -> new ArrayList<>()).add(row.getAttributeValue().getValue());
            }
        }
        for (ProductAttribute attribute : byId.values()) {
            if (named.contains(attribute.getName())) {
                continue;
            }
            for (String value : values.getOrDefault(attribute.getId(), List.of())) {
                result.add(new ScanVariant(attribute.getName(), value));
            }
        }
        return result;
    }

    private String imageUrl(ProductSku sku) {
        if (sku.getImage() != null && sku.getImage().getUrl() != null) {
            return sku.getImage().getUrl();
        }
        List<ProductRepository.ProductImage> images = products.findImageUrls(List.of(sku.getProduct().getId()));
        return images.isEmpty() ? null : images.get(0).getUrl();
    }

    private List<CatalogImageMatcher.ProductMatch> searchNames(String name) {
        if (name == null) {
            return List.of();
        }
        String safe = name.replace("%", "").replace("_", "").trim();
        if (safe.length() < 2) {
            return List.of();
        }
        List<Product> found = products.findForScannerName(safe, PageRequest.of(0, 5));
        if (found.isEmpty()) {
            return List.of();
        }
        List<Long> ids = found.stream().map(Product::getId).toList();
        Map<Long, String> images = new LinkedHashMap<>();
        for (ProductRepository.ProductImage image : products.findImageUrls(ids)) {
            images.putIfAbsent(image.getProductId(), image.getUrl());
        }
        Map<Long, ProductSku> skuByProduct = new LinkedHashMap<>();
        for (ProductSku sku : skus.findForScannerProducts(ids)) {
            ProductSku current = skuByProduct.get(sku.getProduct().getId());
            if (current == null || Boolean.TRUE.equals(sku.getIsDefault())) {
                skuByProduct.put(sku.getProduct().getId(), sku);
            }
        }
        return found.stream().map(product -> {
            ProductSku sku = skuByProduct.get(product.getId());
            String image = sku != null && sku.getImage() != null ? sku.getImage().getUrl() : images.get(product.getId());
            return new CatalogImageMatcher.ProductMatch(product.getId(), sku == null ? null : sku.getId(),
                    product.getName(), sku == null ? null : sku.getSku(), image);
        }).toList();
    }

    private CheckedImage checked(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessLogicException("Image is required");
        }
        if (image.getSize() > MAX_IMAGE_BYTES) {
            throw new BusinessLogicException("Image is too large");
        }
        String mediaType = image.getContentType() == null ? "" : image.getContentType().toLowerCase(Locale.ROOT);
        int separator = mediaType.indexOf(';');
        if (separator >= 0) {
            mediaType = mediaType.substring(0, separator).trim();
        }
        byte[] bytes;
        try {
            bytes = image.getBytes();
        } catch (IOException ex) {
            throw new BusinessLogicException("Image could not be read");
        }
        if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES || !IMAGE_TYPES.contains(mediaType) || !matches(mediaType, bytes)) {
            throw new BusinessLogicException("Image must be JPEG, PNG, or WebP");
        }
        return new CheckedImage(bytes, mediaType);
    }

    private static boolean matches(String mediaType, byte[] bytes) {
        if ("image/jpeg".equals(mediaType)) {
            return bytes.length > 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
        }
        if ("image/png".equals(mediaType)) {
            return bytes.length > 8 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47;
        }
        return bytes.length > 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }

    private static boolean operationalViewer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(authority -> OPERATIONAL.contains(authority.getAuthority()));
    }

    private static String actorKey() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return "anonymous";
        }
        return auth.getName();
    }

    private static long elapsed(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }

    private record LoadedScan(ProductSku sku, Inventory inventory,
                              List<ProductAttributeRepository.AttributeValue> attributeRows,
                              List<VariantAttribute> variantRows, PriceResult price, String imageUrl) {}

    private record CheckedImage(byte[] bytes, String mediaType) {}
}
