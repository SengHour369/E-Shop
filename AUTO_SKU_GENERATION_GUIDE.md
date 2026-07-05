 # Automatic SKU Generation - Usage Guide

## Overview
✅ **YES** - When you create a ProductSku with `sku: null` (not provided), the system **AUTOMATICALLY GENERATES** the SKU code dynamically without requiring manual input.

---

## How It Works

### Code Flow

```
ProductSkuServiceImpl.createSku()
    ↓
Check if request.getSku() is null or blank?
    │
    ├─ YES (sku: null) → Auto-generate using SkuGeneratorUtil
    │   ├─ Extract category code → ELEC
    │   ├─ Extract product code → IPH15
    │   ├─ Extract attributes → BLU, 128
    │   ├─ Combine → ELEC-IPH15-BLU-128
    │   ├─ Check uniqueness
    │   └─ Save to database
    │
    └─ NO (sku: "CUSTOM-SKU") → Use provided value (validate uniqueness)
```

---

## Implementation Details

### Line 50-65 in ProductSkuServiceImpl.createSku()

```java
// 4. Generate SKU if not provided and ensure uniqueness
if (request.getSku() == null || request.getSku().isBlank()) {
    // ✅ SKU NOT PROVIDED → AUTO-GENERATE
    String base = skuGeneratorUtil.generateSku(product, request);
    String candidate = base;
    int suffix = 1;
    while (productSkuRepository.existsBySku(candidate)) {
        candidate = base + "-" + suffix;
        suffix++;
    }
    sku.setSku(candidate);
} else {
    // ✅ SKU PROVIDED → USE IT (VALIDATE UNIQUENESS)
    if (productSkuRepository.existsBySku(request.getSku())) {
        throw new ResourceNotFoundException("SKU already exists: " + request.getSku());
    }
    sku.setSku(request.getSku());
}
```

---

## Usage Examples

### ✅ OPTION 1: NO INPUT - AUTO-GENERATE SKU (Recommended)

**Request JSON**:
```json
{
  "sku": null,
  "description": "Blue - 128GB",
  "price": 999.99,
  "quantity": 40,
  "is_default": false,
  "inventory": {
    "quantity": 40,
    "warehouse_location": "Warehouse B - Shelf B1"
  },
  "product_attributes": [
    {
      "name": "Color",
      "attributes": [{ "value": "Blue" }]
    },
    {
      "name": "Storage",
      "attributes": [{ "value": "128GB" }]
    }
  ]
}
```

**System Response**:
```json
{
  "id": 1,
  "sku": "ELEC-IPH15-BLU-128",  // ← AUTO-GENERATED
  "description": "Blue - 128GB",
  "price": 999.99,
  "is_default": false
}
```

---

### ✅ OPTION 2: MANUAL INPUT - PROVIDE CUSTOM SKU

**Request JSON**:
```json
{
  "sku": "CUSTOM-SKU-001",
  "description": "Blue - 128GB",
  "price": 999.99,
  "quantity": 40
}
```

**System Response**:
```json
{
  "id": 2,
  "sku": "CUSTOM-SKU-001",  // ← USER-PROVIDED
  "description": "Blue - 128GB",
  "price": 999.99,
  "is_default": false
}
```

---

## Postman Collection Examples

### Create Product (Auto SKU Generation)

**Method**: POST  
**URL**: `{{baseUrl}}/api/v1/products/create/`

**Headers**:
```
Authorization: Bearer {{token}}
```

**Form Data**:
```
name: iPhone 15
description: Latest Apple smartphone
sub_category_id: 1
is_active: true

skus: [
  {
    "sku": null,
    "description": "Black - 128GB",
    "price": 999.99,
    "quantity": 50,
    "is_default": true,
    "product_attributes": [
      {
        "name": "Color",
        "attributes": [{ "value": "Black" }]
      },
      {
        "name": "Storage",
        "attributes": [{ "value": "128GB" }]
      }
    ]
  }
]

files: (product image)
```

**Expected Result**:
- Product ID: 1
- SKU Generated: `ELEC-IPH15-BLK-128`

---

## Auto-Generated SKU Format

### Pattern: `CATEGORY-PRODUCT-COLOR-STORAGE`

#### Example Breakdown:

**Input**:
```
Product Name: iPhone 15
Category: Electronics → Smartphones
Attributes:
  - Color: Blue
  - Storage: 128GB
```

**Generated SKU Parts**:
| Part | How Generated | Result |
|------|---------------|--------|
| **ELEC** | Category first 4 letters | Electronics → ELEC |
| **IPH15** | Product name (preserve digits) | iPhone 15 → IPH15 |
| **BLU** | Color value first 3 letters | Blue → BLU |
| **128** | Storage numeric value | 128GB → 128 |
| **FINAL** | Combined with dashes | ELEC-IPH15-BLU-128 |

---

## More Generated SKU Examples

| Product | Category | Color | Storage | Generated SKU |
|---------|----------|-------|---------|---------------|
| iPhone 15 | Electronics | Space Black | 128GB | ELEC-IPH15-BLK-128 |
| iPhone 15 | Electronics | White | 256GB | ELEC-IPH15-WHT-256 |
| Samsung S24 | Electronics | Blue | 512GB | ELEC-SAM-BLU-512 |
| MacBook Pro | Computers | Silver | 1TB | COMP-MBP-SIL-1 |
| Pixel 9 | Electronics | Green | 256GB | ELEC-PIX9-GRE-256 |

---

## Duplicate SKU Handling

### What if Generated SKU Already Exists?

The system automatically appends a numeric suffix:

```
Generated: ELEC-IPH15-BLU-128
Exists? ✓

Retry with suffix:
ELEC-IPH15-BLU-128-1  ← Unique!
```

**Example Process**:
```
Create 1st SKU: sku=null → Generated: ELEC-IPH15-BLU-128 ✓
Create 2nd SKU: sku=null → Generated: ELEC-IPH15-BLU-128-1 ✓
Create 3rd SKU: sku=null → Generated: ELEC-IPH15-BLU-128-2 ✓
```

---

## Update Product SKU Behavior

### When Updating (PUT Request)

**Update with sku: null**
```json
{
  "productSkuId": 1,
  "sku": null,
  "price": 1099.99
}
```
**Result**: SKU is **NOT CHANGED** - original SKU is preserved

**Update with sku: "NEW-SKU"**
```json
{
  "productSkuId": 1,
  "sku": "NEW-SKU",
  "price": 1099.99
}
```
**Result**: SKU is **CHANGED** to "NEW-SKU" (if unique)

---

## DTO Field Definition

### ProductSkuRequest.java

```java
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class ProductSkuRequest {
    Long productSkuId;
    
    private String sku;  // ← Can be null for auto-generation
    
    private String description;
    private BigDecimal price;
    private Long quantity;
    
    @JsonProperty("low_stock_threshold")
    private Integer lowStockThreshold = 5;
    
    @JsonProperty("is_default")
    private Boolean isDefault = false;
    
    private Boolean OperatorProductAttribute = false;
    
    @Valid
    private InventoryRequest inventory;
    
    @JsonProperty("product_attributes")
    private List<ProductAttributeRequest> productAttributes = new ArrayList<>();
}
```

---

## Minimum Required Fields for Auto SKU Generation

### Create ProductSku (sku will be auto-generated)

```json
{
  "sku": null,                    // ← REQUIRED (set to null for auto-generate)
  "price": 999.99,               // ← REQUIRED (must be positive)
  "quantity": 50,                // ← REQUIRED (must be positive)
  "product_attributes": [        // ← REQUIRED (for better SKU generation)
    {
      "name": "Color",
      "attributes": [
        { "value": "Blue" }
      ]
    }
  ]
}
```

---

## Complete Workflow Example

### Step 1: Create Product with Auto-Generated SKUs

**Request**:
```bash
POST /api/v1/products/create/
Authorization: Bearer <token>

Form Data:
- name: Samsung Galaxy S24
- sub_category_id: 1
- skus: [
    {
      "sku": null,
      "price": 899.99,
      "quantity": 50,
      "product_attributes": [
        { "name": "Color", "attributes": [{ "value": "Phantom Black" }] },
        { "name": "Storage", "attributes": [{ "value": "256GB" }] }
      ]
    },
    {
      "sku": null,
      "price": 999.99,
      "quantity": 30,
      "product_attributes": [
        { "name": "Color", "attributes": [{ "value": "Phantom Silver" }] },
        { "name": "Storage", "attributes": [{ "value": "512GB" }] }
      ]
    }
  ]
```

**Response**:
```json
{
  "status": 200,
  "message": "Product created successfully",
  "data": {
    "productId": 5,
    "name": "Samsung Galaxy S24",
    "skus": [
      {
        "id": 101,
        "sku": "ELEC-SAM-PHB-256",  // ← AUTO-GENERATED
        "price": 899.99,
        "description": null
      },
      {
        "id": 102,
        "sku": "ELEC-SAM-PHS-512",  // ← AUTO-GENERATED
        "price": 999.99,
        "description": null
      }
    ]
  }
}
```

---

## Benefits of Auto-Generated SKUs

✅ **No Manual Input Needed** - User doesn't have to think about SKU format  
✅ **Consistent Format** - All SKUs follow the same pattern  
✅ **Unique by Default** - System ensures no duplicates  
✅ **Semantic** - SKU contains product/category/attribute info  
✅ **Scalable** - Works for any product category  
✅ **Flexible** - Can still provide manual SKU if needed  

---

## Summary

| Scenario | User Input | System Behavior |
|----------|-----------|-----------------|
| Create SKU with `sku: null` | ❌ No input | ✅ Auto-generates: `ELEC-IPH15-BLU-128` |
| Create SKU with `sku: "CUSTOM"` | ✅ Manual input | ✅ Uses provided: `CUSTOM` |
| Update SKU with `sku: null` | ❌ No input | ✅ Preserves existing |
| Update SKU with `sku: "NEW"` | ✅ Manual input | ✅ Changes to: `NEW` |

---

**Status**: ✅ ACTIVE - Automatic SKU generation is working and ready to use!

