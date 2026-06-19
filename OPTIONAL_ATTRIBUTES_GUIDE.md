# Product Attributes - Optional Integration Guide

## Overview
Attributes are **completely optional** when creating or updating products and SKUs. You can:
- Create products/SKUs without any attributes
- Add attributes after SKU creation via dedicated attribute endpoints
- Include attributes during SKU creation for convenience

## Request Examples

### Create SKU WITHOUT Attributes (Standard)
```json
POST /api/skus

{
  "sku": "SKU-001",
  "description": "Product variant",
  "price": 29.99,
  "quantity": 100,
  "low_stock_threshold": 5,
  "is_default": true
}
```

### Create SKU WITH Attributes (Optional)
```json
POST /api/skus

{
  "sku": "SKU-001",
  "description": "Red Large Shirt",
  "price": 29.99,
  "quantity": 100,
  "low_stock_threshold": 5,
  "is_default": true,
  "attributes": [
    {
      "attribute_id": 1,
      "attribute_value_id": 5
    },
    {
      "attribute_id": 2,
      "attribute_value_id": 10
    }
  ]
}
```

### Add Attributes to Existing SKU
```json
POST /api/variant-attributes

{
  "sku_id": 1,
  "attribute_id": 1,
  "attribute_value_id": 5
}
```

### Update Attribute Value for SKU
```json
PUT /api/variant-attributes/{id}

{
  "attribute_id": 1,
  "attribute_value_id": 8
}
```

## Response Example

### Get SKU Response WITH Attributes
```json
{
  "id": 1,
  "sku": "SKU-001",
  "description": "Red Large Shirt",
  "price": 29.99,
  "quantity": 100,
  "low_stock_threshold": 5,
  "is_default": true,
  "attributes": [
    {
      "id": 1,
      "product_sku_id": 1,
      "attribute_id": 1,
      "attribute_name": "Color",
      "attribute_value_id": 5,
      "attribute_value": "Red"
    },
    {
      "id": 2,
      "product_sku_id": 1,
      "attribute_id": 2,
      "attribute_name": "Size",
      "attribute_value_id": 10,
      "attribute_value": "Large"
    }
  ]
}
```

## Service Methods for Attributes

### ProductAttributeService
- `createAttribute(String)` - Create a new attribute (e.g., "Color", "Size")
- `getAttributeById(Long)` - Get attribute by ID
- `getAttributeByName(String)` - Get attribute by name
- `getAllAttributes()` - List all attributes
- `updateAttribute(Long, String)` - Update attribute name
- `deleteAttribute(Long)` - Delete attribute

### ProductAttributeValueService
- `createAttributeValue(Long, String)` - Create value for attribute (e.g., "Red" for "Color")
- `getAttributeValueById(Long)` - Get value by ID
- `getValuesByAttributeId(Long)` - List all values for an attribute
- `updateAttributeValue(Long, String)` - Update value
- `deleteAttributeValue(Long)` - Delete value

### VariantAttributeService
- `assignAttributeToVariant(Long, Long, Long)` - Assign attribute to SKU
- `getAttributesByVariantId(Long)` - List all attributes for a SKU
- `updateVariantAttribute(Long, Long)` - Change attribute value for SKU
- `removeAttributeFromVariant(Long)` - Remove single attribute from SKU
- `removeAllAttributesFromVariant(Long)` - Remove all attributes from SKU

## Step-by-Step Setup

### Step 1: Create Attributes
```
POST /api/attributes
Body: { "name": "Color" }

POST /api/attributes
Body: { "name": "Size" }
```

### Step 2: Create Attribute Values
```
POST /api/attribute-values
Body: { "attribute_id": 1, "value": "Red" }

POST /api/attribute-values
Body: { "attribute_id": 1, "value": "Blue" }

POST /api/attribute-values
Body: { "attribute_id": 2, "value": "Small" }

POST /api/attribute-values
Body: { "attribute_id": 2, "value": "Large" }
```

### Step 3A: Create Product SKU WITHOUT Attributes
```
POST /api/product-skus
Body: { "sku": "SKU-001", "price": 29.99, "quantity": 100 }
```

### Step 3B: OR Create Product SKU WITH Attributes
```
POST /api/product-skus
Body: {
  "sku": "SKU-001",
  "price": 29.99,
  "quantity": 100,
  "attributes": [
    { "attribute_id": 1, "attribute_value_id": 1 },
    { "attribute_id": 2, "attribute_value_id": 3 }
  ]
}
```

### Step 4: Add Attributes Later (if not done in Step 3)
```
POST /api/variant-attributes
Body: {
  "sku_id": 1,
  "attribute_id": 1,
  "attribute_value_id": 1
}
```

## Key Design Decisions

✅ **Attributes are completely optional**
- No validation errors if attributes are not provided
- Attributes can be added/updated at any time
- Removing a SKU removes all its attributes via cascade delete

✅ **Flexible assignment**
- Assign attributes during SKU creation or later
- Update attributes without recreating SKU
- Remove individual or all attributes from SKU

✅ **Data integrity**
- Unique constraint: one attribute per SKU (prevents duplicates)
- Unique constraint: one value per attribute (prevents duplicate values)
- Foreign key cascade: deleting parent removes children

✅ **Error handling**
- Duplicate attribute assignment throws error
- Invalid attribute value (not belonging to attribute) throws error
- Non-existent resources throw ResourceNotFoundException

## Database Schema

```sql
-- Already created tables with indices:
CREATE TABLE attributes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE
);

CREATE TABLE attribute_values (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attribute_id BIGINT NOT NULL,
    value VARCHAR(255) NOT NULL,
    FOREIGN KEY (attribute_id) REFERENCES attributes(id) ON DELETE CASCADE,
    UNIQUE KEY uk_attribute_value (attribute_id, value)
);

CREATE TABLE variant_attributes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_sku_id BIGINT NOT NULL,
    attribute_id BIGINT NOT NULL,
    attribute_value_id BIGINT NOT NULL,
    FOREIGN KEY (product_sku_id) REFERENCES product_skus(id) ON DELETE CASCADE,
    FOREIGN KEY (attribute_id) REFERENCES attributes(id) ON DELETE CASCADE,
    FOREIGN KEY (attribute_value_id) REFERENCES attribute_values(id) ON DELETE CASCADE,
    UNIQUE KEY uk_sku_attribute (product_sku_id, attribute_id)
);

CREATE INDEX idx_attribute_values_attr_id ON attribute_values(attribute_id);
CREATE INDEX idx_variant_attributes_sku_id ON variant_attributes(product_sku_id);
CREATE INDEX idx_variant_attributes_attr_id ON variant_attributes(attribute_id);
```

## Notes
- All validation errors will be caught and returned appropriately
- Attributes are not required for product/SKU creation
- You can create a basic product system without using attributes at all
- Attributes are useful for detailed product variations (e.g., e-commerce size/color/brand)

