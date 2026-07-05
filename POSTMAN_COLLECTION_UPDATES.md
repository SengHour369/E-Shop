# Postman Collection - Updated to Match Controller Requirements

## Summary of Updates

The E_Shop_Updated.postman_collection.json has been updated to exactly match the ProductController requirements.

---

## Product Endpoints - Updated

### 1. ✅ Create Product (POST)

**Controller Method**: `ProductController.createProduct()`  
**Line**: 33-74

**Updated Postman Request**:
- **Method**: POST
- **URL**: `{{baseUrl}}/api/v1/products/create/`
- **Content-Type**: multipart/form-data

**Required Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| name | text | ✅ YES | Product name |
| sub_category_id | text | ✅ YES | Subcategory ID |
| files | file | ✅ YES | Product images |

**Optional Parameters**:
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| description | text | - | Product description |
| is_active | text | true | Active status |
| skus | text | - | SKUs JSON array (auto-generates SKU if sku=null) |
| quantity | text | - | Default inventory quantity for SKUs without inventory |
| warehouseLocation | text | - | Default warehouse location |

**Example SKU JSON** (with auto-generation):
```json
[
  {
    "sku": null,
    "description": "Black - 128GB",
    "price": 999.99,
    "quantity": 50,
    "is_default": true,
    "low_stock_threshold": 5,
    "inventory": {
      "quantity": 50,
      "warehouse_location": "Warehouse A"
    },
    "product_attributes": [
      {
        "name": "Color",
        "attributes": [{"value": "Black"}]
      },
      {
        "name": "Storage",
        "attributes": [{"value": "128GB"}]
      }
    ]
  }
]
```

---

### 2. ✅ Update Product (JSON)

**Controller Method**: `ProductController.updateProductJson()`  
**Line**: 118-125

**Updated Postman Request**:
- **Method**: PUT
- **URL**: `{{baseUrl}}/api/v1/products/update/?id=1`
- **Content-Type**: application/json
- **Query Parameter**: id (product ID)

**Body**: ProductRequest JSON

**Example**:
```json
{
  "name": "iPhone 15 Updated",
  "description": "Updated flagship smartphone",
  "is_active": true,
  "sub_category_id": 1,
  "skus": [
    {
      "productSkuId": 1,
      "sku": null,
      "description": "Space Black - 128GB",
      "price": 949.99,
      "quantity": 60,
      "is_default": true,
      "low_stock_threshold": 10,
      "inventory": {
        "quantity": 60,
        "warehouse_location": "Warehouse A"
      }
    }
  ]
}
```

---

### 3. ✅ Update Product (Multipart)

**Controller Method**: `ProductController.updateProduct()`  
**Line**: 76-116

**Updated Postman Request**:
- **Method**: PUT
- **URL**: `{{baseUrl}}/api/v1/products/update/?id=1`
- **Content-Type**: multipart/form-data
- **Query Parameter**: id (product ID)

**Required Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| name | text | Product name |
| sub_category_id | text | Subcategory ID |

**Optional Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| description | text | Product description |
| is_active | text | Active status (default true) |
| skus | text | SKUs JSON array |
| quantity | text | Default inventory quantity |
| warehouseLocation | text | Default warehouse location |
| files | file | Product images |

---

### 4. ✅ Update Product Status (POST)

**Controller Method**: `ProductController.updateProductStatus()`  
**Line**: 127-134

**Updated Postman Request**:
- **Method**: POST (corrected from PATCH)
- **URL**: `{{baseUrl}}/api/v1/products/update/status/?id=1&isActive=true`
- **Query Parameters**:
  - `id`: Product ID
  - `isActive`: true/false

---

### 5. ✅ Delete Product (POST)

**Controller Method**: `ProductController.deleteProduct()`  
**Line**: 136-141

**Updated Postman Request**:
- **Method**: POST (corrected from DELETE)
- **URL**: `{{baseUrl}}/api/v1/products/delete/?id=1`
- **Query Parameter**: id (product ID)

---

## Key Changes Made

| Aspect | Before | After | Status |
|--------|--------|-------|--------|
| Create Product method | POST | POST | ✅ Correct |
| Create Product URL | /api/v1/products/create | /api/v1/products/create/ | ✅ Updated |
| Update Product JSON URL | /api/v1/products/update | /api/v1/products/update/ | ✅ Updated |
| Update Product Multipart URL | /api/v1/products/update | /api/v1/products/update/ | ✅ Updated |
| Update Status method | PATCH | POST | ✅ Fixed |
| Update Status URL | /api/v1/products/id/status | /api/v1/products/update/status/ | ✅ Fixed |
| Delete method | DELETE | POST | ✅ Fixed |
| Delete URL | /api/v1/products/delete | /api/v1/products/delete/ | ✅ Fixed |

---

## Auto SKU Generation Feature

Both Create and Update Product requests now support **automatic SKU generation**:

### How it works:
1. Set `"sku": null` in the SKU request
2. System automatically generates: `CATEGORY-PRODUCT-COLOR-STORAGE`
3. Example: `ELEC-IPH15-BLU-128`

### Manual Example (Auto SKU):
```json
{
  "sku": null,
  "description": "Blue - 128GB",
  "price": 999.99,
  "quantity": 40,
  "product_attributes": [
    {"name": "Color", "attributes": [{"value": "Blue"}]},
    {"name": "Storage", "attributes": [{"value": "128GB"}]}
  ]
}
```

**Result**: SKU generated as `ELEC-IPH15-BLU-128`

---

## Testing Checklist

✅ **Create Product**
- [ ] POST request to /api/v1/products/create/
- [ ] Include required: name, sub_category_id, files
- [ ] Include optional: description, is_active, skus
- [ ] SKUs with sku=null should auto-generate

✅ **Update Product (JSON)**
- [ ] PUT request to /api/v1/products/update/?id=1
- [ ] Include required: name, sub_category_id
- [ ] Include optional: sku (if updating), skus array
- [ ] sku=null preserves existing SKU

✅ **Update Product (Multipart)**
- [ ] PUT request to /api/v1/products/update/?id=1
- [ ] Include required form params: name, sub_category_id
- [ ] Include optional: files, skus

✅ **Update Product Status**
- [ ] POST request to /api/v1/products/update/status/?id=1&isActive=true
- [ ] Query params: id, isActive

✅ **Delete Product**
- [ ] POST request to /api/v1/products/delete/?id=1
- [ ] Query param: id

---

## Header Requirements

All product endpoints (except public ones) require:
```
Authorization: Bearer {{token}}
```

---

## Response Format

All endpoints return:
```json
{
  "status": 200,
  "message": "Success message",
  "data": {
    // Response data
  }
}
```

---

## Status: ✅ COMPLETE

The Postman collection has been fully updated to match the ProductController requirements exactly.

All endpoints now use:
- ✅ Correct HTTP methods
- ✅ Correct URL paths with trailing slashes
- ✅ Correct parameter names and types
- ✅ Proper multipart/form-data and JSON content types
- ✅ Auto SKU generation support
- ✅ Full documentation and descriptions

