package com.example.learning_spring_security.utils;

import com.example.learning_spring_security.Model.Product;
import com.example.learning_spring_security.Model.SubCategory;
import com.example.learning_spring_security.dto.Request.ProductAttributeRequest;
import com.example.learning_spring_security.dto.Request.ProductAttributeValueRequest;
import com.example.learning_spring_security.dto.Request.ProductSkuRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dynamic SKU Generator Utility for generating unique Stock Keeping Unit codes.
 * 
 * The SKU generator supports multiple strategies:
 * 1. Product Code Only: PROD-00001 (sequential)
 * 2. Category-Product Code: ELEC-IPH-00001
 * 3. Attribute-Based: IPH15-BLU-128 (product code with attribute values)
 * 4. Full Pattern: ELEC-IPH15-BLU-128GB
 * 
 * Example:
 * - Product: "iPhone 15"
 * - Category: "Electronics"
 * - SubCategory: "Smartphones"
 * - Attributes: Color (Blue), Storage (128GB)
 * - Generated SKU: ELEC-IPH15-BLU-128
 * 
 * @author E-Shop Team
 */
@Slf4j
@Component
public class SkuGeneratorUtil {

    public static final String DEFAULT_PREFIX = "PRD";
    public static final int DEFAULT_MAX_CODE_LENGTH = 10;
    public static final String DELIMITER = "-";

    /**
     * Generate SKU using full product and attribute information.
     * 
     * @param product Product entity
     * @param request ProductSkuRequest with attributes
     * @return Generated SKU code (e.g., "ELEC-IPH15-BLU-128")
     */
    public String generateSku(Product product, ProductSkuRequest request) {
        SkuConfig config = SkuConfig.builder()
                .includeCategory(true)
                .includeAttributes(true)
                .maxProductCodeLength(5)
                .delimiter(DELIMITER)
                .build();
        
        return generateSkuWithConfig(product, request, config);
    }

    /**
     * Generate SKU with custom configuration.
     * 
     * @param product Product entity
     * @param request ProductSkuRequest
     * @param config Customization configuration
     * @return Generated SKU
     */
    public String generateSkuWithConfig(Product product, ProductSkuRequest request, SkuConfig config) {
        StringBuilder skuBuilder = new StringBuilder();
        
        // 1. Add category code if requested
        if (config.isIncludeCategory() && product.getSubCategory() != null) {
            String categoryCode = extractCategoryCode(product.getSubCategory(), config);
            if (categoryCode != null && !categoryCode.isEmpty()) {
                skuBuilder.append(categoryCode).append(config.getDelimiter());
            }
        }
        
        // 2. Add product code
        String productCode = extractProductCode(product.getName(), config);
        skuBuilder.append(productCode);
        
        // 3. Add attribute codes if requested
        if (config.isIncludeAttributes() && request != null && request.getProductAttributes() != null) {
            List<String> attributeCodes = extractAttributeCodes(request.getProductAttributes(), config);
            for (String attrCode : attributeCodes) {
                skuBuilder.append(config.getDelimiter()).append(attrCode);
            }
        }
        
        log.debug("Generated base SKU: {}", skuBuilder);
        return skuBuilder.toString();
    }

    /**
     * Generate SKU with attribute details as a map.
     * Useful when you want to track which attributes were used.
     * 
     * @param product Product entity
     * @param request ProductSkuRequest
     * @return SKU with component breakdown
     */
    public SkuComponents generateSkuComponents(Product product, ProductSkuRequest request) {
        SkuConfig config = SkuConfig.builder()
                .includeCategory(true)
                .includeAttributes(true)
                .maxProductCodeLength(5)
                .delimiter(DELIMITER)
                .build();
        
        return generateSkuComponentsWithConfig(product, request, config);
    }

    /**
     * Generate SKU and return component details.
     */
    public SkuComponents generateSkuComponentsWithConfig(Product product, ProductSkuRequest request, SkuConfig config) {
        SkuComponents.SkuComponentsBuilder builder = SkuComponents.builder();
        
        // Category code
        String categoryCode = null;
        if (config.isIncludeCategory() && product.getSubCategory() != null) {
            categoryCode = extractCategoryCode(product.getSubCategory(), config);
            builder.categoryCode(categoryCode);
        }
        
        // Product code
        String productCode = extractProductCode(product.getName(), config);
        builder.productCode(productCode);
        
        // Attribute codes
        Map<String, String> attributeCodes = new LinkedHashMap<>();
        if (config.isIncludeAttributes() && request != null && request.getProductAttributes() != null) {
            attributeCodes = extractAttributeCodesAsMap(request.getProductAttributes(), config);
            builder.attributeCodes(attributeCodes);
        }
        
        // Build final SKU
        StringBuilder sku = new StringBuilder();
        if (categoryCode != null && !categoryCode.isEmpty()) {
            sku.append(categoryCode).append(config.getDelimiter());
        }
        sku.append(productCode);
        for (String code : attributeCodes.values()) {
            if (code != null && !code.isEmpty()) {
                sku.append(config.getDelimiter()).append(code);
            }
        }
        
        builder.finalSku(sku.toString());
        return builder.build();
    }

    /**
     * Extract category code from SubCategory.
     * Example: "Electronics" -> "ELEC"
     */
    private String extractCategoryCode(SubCategory subCategory, SkuConfig config) {
        if (subCategory == null || subCategory.getCategory() == null) {
            return null;
        }
        
        String categoryName = subCategory.getCategory().getName();
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return null;
        }
        
        // Remove special characters and uppercase
        String cleaned = categoryName.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        int maxLen = Math.min(config.getMaxCategoryCodeLength(), cleaned.length());
        return cleaned.substring(0, maxLen);
    }

    /**
     * Extract product code from product name.
     * Tries to preserve trailing digits for pattern matching.
     * Example: "iPhone 15" -> "IPH15"
     */
    private String extractProductCode(String name, SkuConfig config) {
        String normalized = name == null ? "" : name;
        
        // Remove special characters and uppercase
        String cleaned = normalized.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (cleaned.isEmpty()) {
            return config.getDefaultPrefix();
        }
        
        // Try to extract letters + trailing digits pattern
        Pattern trailingDigitsPattern = Pattern.compile("([A-Z]{1,})(\\d+)$");
        Matcher matcher = trailingDigitsPattern.matcher(cleaned);
        
        if (matcher.find()) {
            String letters = matcher.group(1);
            String digits = matcher.group(2);
            
            // Limit letters to max length
            int maxLetters = config.getMaxProductCodeLength() - Math.min(digits.length(), 3);
            String limitedLetters = letters.length() > maxLetters 
                    ? letters.substring(0, Math.max(1, maxLetters)) 
                    : letters;
            
            return limitedLetters + digits;
        } else {
            // No trailing digits, just truncate
            int maxLen = Math.min(config.getMaxProductCodeLength(), cleaned.length());
            return cleaned.substring(0, maxLen);
        }
    }

    /**
     * Extract attribute codes and return as ordered list.
     * Example: [Color: Blue -> "BLU", Storage: 128GB -> "128"]
     */
    private List<String> extractAttributeCodes(List<ProductAttributeRequest> attributes, SkuConfig config) {
        List<String> codes = new ArrayList<>();
        
        if (attributes == null || attributes.isEmpty()) {
            return codes;
        }
        
        for (ProductAttributeRequest attr : attributes) {
            if (attr.getName() == null || attr.getAttributes() == null || attr.getAttributes().isEmpty()) {
                continue;
            }
            
            String attrName = attr.getName().toLowerCase();
            ProductAttributeValueRequest firstValue = attr.getAttributes().get(0);
            String value = firstValue != null ? firstValue.getValue() : null;
            
            if (value == null || value.isEmpty()) {
                continue;
            }
            
            String code = null;
            
            // Color attributes
            if (attrName.contains("color")) {
                code = extractColorCode(value, config);
            }
            // Storage/Size/Memory attributes
            else if (attrName.contains("storage") || attrName.contains("size") || attrName.contains("memory")) {
                code = extractStorageCode(value, config);
            }
            // Size (clothing)
            else if (attrName.contains("size") || attrName.contains("dimension")) {
                code = extractSizeCode(value, config);
            }
            // Brand
            else if (attrName.contains("brand")) {
                code = extractBrandCode(value, config);
            }
            // Generic attribute
            else {
                code = extractGenericAttributeCode(value, config);
            }
            
            if (code != null && !code.isEmpty()) {
                codes.add(code);
            }
        }
        
        return codes;
    }

    /**
     * Extract attribute codes as a map for detailed tracking.
     */
    private Map<String, String> extractAttributeCodesAsMap(List<ProductAttributeRequest> attributes, SkuConfig config) {
        Map<String, String> codes = new LinkedHashMap<>();
        
        if (attributes == null || attributes.isEmpty()) {
            return codes;
        }
        
        for (ProductAttributeRequest attr : attributes) {
            if (attr.getName() == null || attr.getAttributes() == null || attr.getAttributes().isEmpty()) {
                continue;
            }
            
            String attrName = attr.getName().toLowerCase();
            ProductAttributeValueRequest firstValue = attr.getAttributes().get(0);
            String value = firstValue != null ? firstValue.getValue() : null;
            
            if (value == null || value.isEmpty()) {
                continue;
            }
            
            String code = null;
            
            if (attrName.contains("color")) {
                code = extractColorCode(value, config);
            }
            else if (attrName.contains("storage") || attrName.contains("size") || attrName.contains("memory")) {
                code = extractStorageCode(value, config);
            }
            else if (attrName.contains("size") || attrName.contains("dimension")) {
                code = extractSizeCode(value, config);
            }
            else if (attrName.contains("brand")) {
                code = extractBrandCode(value, config);
            }
            else {
                code = extractGenericAttributeCode(value, config);
            }
            
            if (code != null && !code.isEmpty()) {
                codes.put(attrName, code);
            }
        }
        
        return codes;
    }

    /**
     * Extract color code.
     * Example: "Space Black" -> "BLK", "Blue" -> "BLU"
     */
    private String extractColorCode(String value, SkuConfig config) {
        String cleaned = value.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (cleaned.isEmpty()) return null;
        
        int maxLen = Math.min(config.getMaxAttributeCodeLength(), cleaned.length());
        return cleaned.substring(0, maxLen);
    }

    /**
     * Extract storage/memory code.
     * Example: "128GB" -> "128", "256 GB" -> "256"
     */
    private String extractStorageCode(String value, SkuConfig config) {
        // Try to extract numeric value first
        Matcher digitMatcher = Pattern.compile("(\\d+)").matcher(value);
        if (digitMatcher.find()) {
            return digitMatcher.group(1);
        }
        
        // Fallback to generic extraction
        return extractGenericAttributeCode(value, config);
    }

    /**
     * Extract size code (for clothing).
     * Example: "Medium" -> "MD", "Small" -> "SM", "XL" -> "XL"
     */
    private String extractSizeCode(String value, SkuConfig config) {
        String cleaned = value.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (cleaned.isEmpty()) return null;
        
        // For single letters, return as is
        if (cleaned.length() == 1) {
            return cleaned;
        }
        
        // For multi-letter sizes, take first 2 chars
        int maxLen = Math.min(2, config.getMaxAttributeCodeLength());
        return cleaned.substring(0, maxLen);
    }

    /**
     * Extract brand code.
     * Example: "Apple" -> "APP", "Samsung" -> "SAM"
     */
    private String extractBrandCode(String value, SkuConfig config) {
        String cleaned = value.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (cleaned.isEmpty()) return null;
        
        int maxLen = Math.min(config.getMaxAttributeCodeLength(), cleaned.length());
        return cleaned.substring(0, maxLen);
    }

    /**
     * Extract generic attribute code.
     * Example: "Wireless" -> "WIR"
     */
    private String extractGenericAttributeCode(String value, SkuConfig config) {
        String cleaned = value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (cleaned.isEmpty()) return null;
        
        int maxLen = Math.min(config.getMaxAttributeCodeLength(), cleaned.length());
        return cleaned.substring(0, maxLen);
    }

    /**
     * Generate a simple sequential SKU by product ID.
     * Example: "PRD-00001"
     */
    public String generateSimpleSequentialSku(Long productId) {
        String prefix = DEFAULT_PREFIX;
        String sequence = String.format("%05d", productId);
        return prefix + DELIMITER + sequence;
    }

    /**
     * Normalize SKU text (uppercase, remove spaces, etc.)
     */
    public String normalizeSku(String sku) {
        if (sku == null) return "";
        return sku.replaceAll("\\s+", "").toUpperCase();
    }

    /**
     * SKU Configuration for customizing generation behavior.
     */
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class SkuConfig {
        private boolean includeCategory = true;
        private boolean includeAttributes = true;
        
        @Builder.Default
        private int maxProductCodeLength = 5;
        
        @Builder.Default
        private int maxCategoryCodeLength = 4;
        
        @Builder.Default
        private int maxAttributeCodeLength = 3;
        
        @Builder.Default
        private String delimiter = "-";
        
        @Builder.Default
        private String defaultPrefix = "PRD";
    }

    /**
     * SKU Components breakdown for detailed information.
     */
    @Getter
    @Setter
    @Builder
    public static class SkuComponents {
        private String categoryCode;
        private String productCode;
        private Map<String, String> attributeCodes;
        private String finalSku;

        @Override
        public String toString() {
            return "SkuComponents{" +
                    "categoryCode='" + categoryCode + '\'' +
                    ", productCode='" + productCode + '\'' +
                    ", attributeCodes=" + attributeCodes +
                    ", finalSku='" + finalSku + '\'' +
                    '}';
        }
    }
}

