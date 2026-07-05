# Dynamic SKU Generator Implementation Guide

## Overview
A comprehensive SKU (Stock Keeping Unit) generator utility has been created in `SkuGeneratorUtil.java` that dynamically generates unique product codes based on product information, categories, and attributes.

---

## File Locations
- **Utility Class**: `D:\spring boot\E_Shop\src\main\java\com\example\learning_spring_security\utils\SkuGeneratorUtil.java`
- **Service Integration**: `D:\spring boot\E_Shop\src\main\java\com\example\learning_spring_security\Service\ServiceImplement\ProductSkuServiceImpl.java`

---

## Features

### 1. **Multiple Generation Strategies**
- ✅ Dynamic attribute-based generation (Default)
- ✅ Category-inclusive generation
- ✅ Custom configuration support
- ✅ Component breakdown tracking
- ✅ Simple sequential generation

### 2. **Attribute Recognition**
Automatically recognizes and extracts codes for:
- **Color**: "Space Black" → BLK, "Blue" → BLU
- **Storage/Memory**: "128GB" → 128, "256GB" → 256
- **Size**: "Medium" → MD, "XL" → XL
- **Brand**: "Apple" → APP, "Samsung" → SAM
- **Generic Attributes**: Customizable extraction

### 3. **SKU Pattern Examples**

```
Product: iPhone 15
Category: Electronics
Attributes: Color (Blue), Storage (128GB)

Generated SKUs:
├─ ELEC-IPH15-BLU-128 (Full: Category + Product + Color + Storage)
├─ IPH15-BLU-128 (Without category)
├─ IPH15 (Product code only)
└─ PRD-00001 (Simple sequential fallback)
```

---

## Usage Examples

### Example 1: Basic SKU Generation (Automatic)
```java
// Automatically called in ProductSkuServiceImpl.createSku()
@Service
public class ProductSkuServiceImpl implements ProductSkuService {
    
    private final SkuGeneratorUtil skuGeneratorUtil;
    
    @Override
    @Transactional
    public ProductSku createSku(Long productId, ProductSkuRequest request) {
        Product product = productRepository.findById(productId).orElseThrow();
        
        // SKU generated automatically if not provided
        if (request.getSku() == null || request.getSku().isBlank()) {
            String base = skuGeneratorUtil.generateSku(product, request);
            // Handle uniqueness...
            sku.setSku(base);
        }
        
        return productSkuRepository.save(sku);
    }
}
```

---

### Example 2: Custom Configuration
```java
// Create custom SKU configuration
SkuGeneratorUtil.SkuConfig customConfig = SkuGeneratorUtil.SkuConfig.builder()
    .includeCategory(true)
    .includeAttributes(true)
    .maxProductCodeLength(6)
    .maxCategoryCodeLength(3)
    .maxAttributeCodeLength(4)
    .delimiter("-")
    .defaultPrefix("PROD")
    .build();

// Generate SKU with custom config
String customSku = skuGeneratorUtil.generateSkuWithConfig(product, request, customConfig);
// Result: "ELEC-iPH15-BLUE-128G"
```

---

### Example 3: Component Breakdown
```java
// Get detailed component information
SkuGeneratorUtil.SkuComponents components = 
    skuGeneratorUtil.generateSkuComponents(product, request);

System.out.println("Category Code: " + components.getCategoryCode());      // ELEC
System.out.println("Product Code: " + components.getProductCode());        // IPH15
System.out.println("Attribute Codes: " + components.getAttributeCodes());  // {color=BLU, storage=128}
System.out.println("Final SKU: " + components.getFinalSku());              // ELEC-IPH15-BLU-128
```

---

### Example 4: Processing Your Sample SKU
**User-provided sample with the SKU generator:**

#### Request JSON:
```json
{
  "sku": null,
  "description": "Blue - 128GB",
  "price": 999.99,
  "quantity": 40,
  "is_default": false,
  "operatorProductAttribute": false,
  "low_stock_threshold": 10,
  "inventory": {
    "quantity": 40,
    "warehouse_location": "Warehouse B - Shelf B1"
  },
  "product_attributes": [
    {
      "id": 1,
      "name": "Color",
      "attributes": [
        {
          "id": 2,
          "value": "Blue"
        }
      ]
    },
    {
      "id": 2,
      "name": "Storage",
      "attributes": [
        {
          "id": 3,
          "value": "128GB"
        }
      ]
    }
  ]
}
```

#### Generated SKU Output:
```
If product "iPhone 15" in "Electronics" > "Smartphones" category:

Default Generation (with category):
  → ELEC-IPH15-BLU-128

Without Category:
  → IPH15-BLU-128

Components Breakdown:
  → categoryCode: "ELEC"
  → productCode: "IPH15"
  → attributeCodes: {
      "color": "BLU",
      "storage": "128"
    }
```

---

### Example 5: Service Method Invocation Pattern

#### Step 1: Product Creation Request
```java
@PostMapping(value = "/create/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
@PreAuthorize("hasAuthority('ADMIN')")
public ResponseEntity<ResponseErrorTemplate> createProduct(
        @RequestParam String name,
        @RequestParam Long sub_category_id,
        @RequestParam(required = false) String skus,
        @RequestParam(value = "files") List<MultipartFile> files) throws Exception {
    
    // Parse SKUs from JSON string
    ObjectMapper mapper = new ObjectMapper();
    List<ProductSkuRequest> skuList = mapper.readValue(skus, 
        mapper.getTypeFactory().constructCollectionType(List.class, ProductSkuRequest.class));
    
    ProductRequest request = new ProductRequest();
    request.setSkus(skuList);
    
    // Service handles SKU generation automatically
    return productService.createProduct(request, files);
}
```

#### Step 2: Service Processing
```java
@Service
public class ProductServiceImpl {
    
    @Transactional
    public ResponseErrorTemplate createProduct(ProductRequest request, List<MultipartFile> files) {
        Product product = productRepository.save(mapToProduct(request));
        
        // For each SKU in request
        for (ProductSkuRequest skuRequest : request.getSkus()) {
            // ProductSkuServiceImpl.createSku() is called
            // SKU is generated automatically if not provided
            productSkuService.createSku(product.getId(), skuRequest);
        }
        
        return new ResponseErrorTemplate(200, "Product created successfully");
    }
}
```

---

## Postman Collection Example

### Create Product (With Automatic SKU Generation)
```json
{
  "name": "POST",
  "url": "{{baseUrl}}/api/v1/products/create/",
  "headers": {
    "Authorization": "Bearer {{token}}",
    "Content-Type": "multipart/form-data"
  },
  "body": {
    "formdata": [
      { "key": "name", "value": "iPhone 15" },
      { "key": "description", "value": "Latest Apple smartphone" },
      { "key": "sub_category_id", "value": 1 },
      { "key": "is_active", "value": "true" },
      { 
        "key": "skus", 
        "value": "[{\"sku\":null,\"description\":\"Blue - 128GB\",\"price\":999.99,\"quantity\":40,\"is_default\":false,\"low_stock_threshold\":10,\"inventory\":{\"quantity\":40,\"warehouse_location\":\"Warehouse B - Shelf B1\"},\"product_attributes\":[{\"name\":\"Color\",\"attributes\":[{\"value\":\"Blue\"}]},{\"name\":\"Storage\",\"attributes\":[{\"value\":\"128GB\"}]}]}]",
        "type": "text"
      }
    ]
  }
}
```

**Generated SKU**: `ELEC-IPH15-BLU-128` (or auto-incremented if duplicate)

---

## Configuration Classes

### SkuConfig
```java
SkuGeneratorUtil.SkuConfig config = SkuGeneratorUtil.SkuConfig.builder()
    .includeCategory(true)              // Include category code
    .includeAttributes(true)            // Include attribute codes
    .maxProductCodeLength(5)            // Max letters in product code
    .maxCategoryCodeLength(4)           // Max letters in category code
    .maxAttributeCodeLength(3)          // Max letters per attribute
    .delimiter("-")                     // Separator between parts
    .defaultPrefix("PRD")               // Fallback prefix
    .build();
```

### SkuComponents
```java
SkuGeneratorUtil.SkuComponents components = 
    skuGeneratorUtil.generateSkuComponentsWithConfig(product, request, config);

// Access component details:
components.getCategoryCode();    // "ELEC"
components.getProductCode();     // "IPH15"
components.getAttributeCodes();  // Map of attribute codes
components.getFinalSku();        // "ELEC-IPH15-BLU-128"
```

---

## Flow Diagram

```
ProductSkuServiceImpl.createSku()
    ↓
Check if SKU provided?
    ├─ YES: Validate uniqueness and use provided
    └─ NO: Call SkuGeneratorUtil.generateSku()
            ↓
            Extract category code (Electronics → ELEC)
            ↓
            Extract product code (iPhone 15 → IPH15)
            ↓
            Extract attribute codes (Blue → BLU, 128GB → 128)
            ↓
            Combine: ELEC-IPH15-BLU-128
            ↓
            Check uniqueness and add suffix if needed
            ↓
            Set SKU on ProductSku entity
            ↓
            Save to database
```

---

## Advanced Features

### 1. Simple Sequential Generation
```java
// For products without attributes
String skuSequential = skuGeneratorUtil.generateSimpleSequentialSku(productId);
// Result: PRD-00001, PRD-00002, etc.
```

### 2. SKU Normalization
```java
String normalized = skuGeneratorUtil.normalizeSku("iPhone 15-BLU 256");
// Result: IPHONE15BLU256
```

### 3. Attribute Mapping Recognition
The generator intelligently recognizes:
- **Color Attributes**: color, colour, shade, tone
- **Storage Attributes**: storage, size, memory, capacity, gb, tb
- **Size Attributes**: size, dimension, fit, length, width, height
- **Brand Attributes**: brand, manufacturer, maker

---

## Error Handling

```java
try {
    String sku = skuGeneratorUtil.generateSku(product, request);
    
    // Validate uniqueness
    while (productSkuRepository.existsBySku(sku)) {
        sku = sku + "-" + counter++;
    }
    
    productSku.setSku(sku);
} catch (Exception e) {
    log.error("SKU generation failed: {}", e.getMessage());
    // Fallback to sequential
    productSku.setSku(skuGeneratorUtil.generateSimpleSequentialSku(productId));
}
```

---

## Integration Checklist

✅ **Completed**:
- [x] SkuGeneratorUtil class created
- [x] ProductSkuServiceImpl updated to use SkuGeneratorUtil
- [x] Dependency injection configured
- [x] Support for category-based SKU generation
- [x] Support for attribute-based SKU generation
- [x] Customizable configuration
- [x] Component breakdown tracking

✅ **Ready for Use**:
- [x] Automatic SKU generation in product creation
- [x] Automatic SKU generation in product updates
- [x] Uniqueness validation and suffix handling
- [x] Postman collection supports null SKU (auto-generate)

---

## Example Test Case

```java
@Test
public void testSkuGenerationWithAttributes() {
    // Setup
    Product product = new Product();
    product.setName("iPhone 15");
    product.setSubCategory(new SubCategory());
    product.getSubCategory().setCategory(new Category());
    product.getSubCategory().getCategory().setName("Electronics");
    
    ProductSkuRequest request = new ProductSkuRequest();
    request.setSku(null); // Trigger auto-generation
    
    ProductAttributeRequest colorAttr = new ProductAttributeRequest();
    colorAttr.setName("Color");
    ProductAttributeValueRequest colorValue = new ProductAttributeValueRequest();
    colorValue.setValue("Blue");
    colorAttr.setAttributes(List.of(colorValue));
    
    ProductAttributeRequest storageAttr = new ProductAttributeRequest();
    storageAttr.setName("Storage");
    ProductAttributeValueRequest storageValue = new ProductAttributeValueRequest();
    storageValue.setValue("128GB");
    storageAttr.setAttributes(List.of(storageValue));
    
    request.setProductAttributes(List.of(colorAttr, storageAttr));
    
    // Execute
    SkuGeneratorUtil.SkuComponents result = 
        skuGeneratorUtil.generateSkuComponentsWithConfig(product, request, config);
    
    // Assert
    assertEquals("ELEC", result.getCategoryCode());
    assertEquals("IPH15", result.getProductCode());
    assertEquals("BLU", result.getAttributeCodes().get("color"));
    assertEquals("128", result.getAttributeCodes().get("storage"));
    assertEquals("ELEC-IPH15-BLU-128", result.getFinalSku());
}
```

---

## Summary

The **SkuGeneratorUtil** provides a flexible, extensible, and intelligent SKU generation system that:
- ✅ Automatically generates SKUs from product name, category, and attributes
- ✅ Supports customizable configurations
- ✅ Ensures SKU uniqueness by adding numeric suffixes
- ✅ Provides detailed component breakdown
- ✅ Integrates seamlessly with ProductSkuServiceImpl
- ✅ Handles edge cases and null values gracefully

The user-provided example SKU (`sku: "IPH15-BLU-128"`) is now automatically generated by the system when a null SKU is provided in requests.

