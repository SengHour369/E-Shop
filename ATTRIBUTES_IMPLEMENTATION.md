# Product Attributes System - Implementation Summary

## Overview
This implementation adds a comprehensive product attributes system to the E-Shop application, allowing product SKUs to have dynamic attributes (e.g., color, size, etc.) with predefined values.

## Created Files

### Model Entities (3 files)

#### 1. ProductAttribute.java
- Location: `src/main/java/com/example/learning_spring_security/Model/`
- Table: `attributes`
- Fields: `id` (PK), `name` (unique)
- Purpose: Stores attribute names (e.g., "Color", "Size", "Brand")

#### 2. ProductAttributeValue.java  
- Location: `src/main/java/com/example/learning_spring_security/Model/`
- Table: `attribute_values`
- Fields: `id` (PK), `value`, `attribute_id` (FK)
- Unique Constraint: `(attribute_id, value)` - prevents duplicate values per attribute
- Purpose: Stores specific values for each attribute (e.g., "Red", "Blue" for "Color")

#### 3. VariantAttribute.java
- Location: `src/main/java/com/example/learning_spring_security/Model/`
- Table: `variant_attributes`
- Fields: `id` (PK), `product_sku_id` (FK), `attribute_id` (FK), `attribute_value_id` (FK)
- Unique Constraint: `(product_sku_id, attribute_id)` - one attribute per SKU
- Purpose: Links ProductSku to specific attribute values (e.g., SKU-123 has Color=Red, Size=Large)

### Repository Interfaces (3 files)

#### 1. ProductAttributeRepository.java
Custom queries:
- `findByNameIgnoreCase(String)` - Case-insensitive name search
- `existsByNameIgnoreCase(String)` - Check for duplicate names
- `findAllOrderByName()` - Sorted list of all attributes

#### 2. ProductAttributeValueRepository.java
Custom queries:
- `findByAttributeIdAndValueIgnoreCase(Long, String)` - Find specific value for attribute
- `findByAttributeId(Long)` - List all values for an attribute
- `existsByAttributeIdAndValue(Long, String)` - Check for duplicate values per attribute

#### 3. VariantAttributeRepository.java
Custom queries:
- `findByProductSkuId(Long)` - Get all attributes for a SKU
- `findByProductSkuIdAndAttributeId(Long, Long)` - Check if attribute already assigned
- `existsByProductSkuIdAndAttributeId(Long, Long)` - Exists check
- `findByProductSkuIdAndAttributeValueId(Long, Long)` - Find by SKU and value

### Service Interfaces (3 files)
Location: `src/main/java/com/example/learning_spring_security/Service/ServiceStructure/`

#### 1. ProductAttributeService
Methods:
- `createAttribute(String)` - Create new attribute
- `getAttributeById(Long)` - Fetch by ID
- `getAttributeByName(String)` - Fetch by name
- `getAllAttributes()` - List all attributes
- `updateAttribute(Long, String)` - Update attribute name
- `deleteAttribute(Long)` - Delete attribute

#### 2. ProductAttributeValueService
Methods:
- `createAttributeValue(Long, String)` - Create value for attribute
- `getAttributeValueById(Long)` - Fetch by ID
- `getValuesByAttributeId(Long)` - List values for attribute
- `updateAttributeValue(Long, String)` - Update value
- `deleteAttributeValue(Long)` - Delete value
- `getAttributeValueByAttributeAndValue(Long, String)` - Fetch specific value

#### 3. VariantAttributeService
Methods:
- `assignAttributeToVariant(Long, Long, Long)` - Assign attribute value to SKU
- `getVariantAttribute(Long)` - Fetch by ID
- `getAttributesByVariantId(Long)` - List attributes for SKU
- `updateVariantAttribute(Long, Long)` - Change attribute value for SKU
- `removeAttributeFromVariant(Long)` - Delete assignment
- `removeAllAttributesFromVariant(Long)` - Clear all attributes from SKU

### Service Implementations (3 files)
Location: `src/main/java/com/example/learning_spring_security/Service/ServiceImplement/`

#### 1. ProductAttributeServiceImpl
- Implements `ProductAttributeService`
- Full CRUD operations with error handling
- Transaction management with `@Transactional`

#### 2. ProductAttributeValueServiceImpl
- Implements `ProductAttributeValueService`
- Validates that attribute values belong to correct attribute
- Prevents duplicate values per attribute (via unique constraint + service check)

#### 3. VariantAttributeServiceImpl
- Implements `VariantAttributeService`
- Validates attribute value ownership before assignment
- Prevents duplicate attribute assignments to same SKU
- All related exceptions caught and handled

### Data Transfer Objects (3 files)
Location: `src/main/java/com/example/learning_spring_security/dto/Response/`

#### 1. ProductAttributeResponse
- Fields: `id`, `name`

#### 2. ProductAttributeValueResponse
- Fields: `id`, `value`, `attributeId`, `attributeName`

#### 3. VariantAttributeResponse
- Fields: `id`, `productSkuId`, `attributeId`, `attributeName`, `attributeValueId`, `attributeValue`

### Mappers (3 files)
Location: `src/main/java/com/example/learning_spring_security/ServiceMapper/`

#### 1. ProductAttributeMapper
- `toEntity(String)` - String → Entity
- `toResponse(ProductAttribute)` - Entity → ResponseErrorTemplate
- `toResponseDTO(ProductAttribute)` - Entity → DTO
- `updateEntity(ProductAttribute, String)` - Update entity fields

#### 2. ProductAttributeValueMapper
- `toResponse(ProductAttributeValue)` - Entity → ResponseErrorTemplate
- `toResponseDTO(ProductAttributeValue)` - Entity → DTO
- `updateEntity(ProductAttributeValue, String)` - Update value field

#### 3. VariantAttributeMapper
- `toResponse(VariantAttribute)` - Entity → ResponseErrorTemplate  
- `toResponseDTO(VariantAttribute)` - Entity → DTO

## Database Schema

### SQL Migration (Flyway/Liquibase recommended)

```sql
-- Create attributes table
CREATE TABLE attributes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create attribute_values table
CREATE TABLE attribute_values (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attribute_id BIGINT NOT NULL,
    value VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (attribute_id) REFERENCES attributes(id) ON DELETE CASCADE,
    UNIQUE KEY uk_attribute_value (attribute_id, value)
);

-- Create variant_attributes table (junction table)
CREATE TABLE variant_attributes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_sku_id BIGINT NOT NULL,
    attribute_id BIGINT NOT NULL,
    attribute_value_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_sku_id) REFERENCES product_skus(id) ON DELETE CASCADE,
    FOREIGN KEY (attribute_id) REFERENCES attributes(id) ON DELETE CASCADE,
    FOREIGN KEY (attribute_value_id) REFERENCES attribute_values(id) ON DELETE CASCADE,
    UNIQUE KEY uk_sku_attribute (product_sku_id, attribute_id)
);

CREATE INDEX idx_attribute_values_attr_id ON attribute_values(attribute_id);
CREATE INDEX idx_variant_attributes_sku_id ON variant_attributes(product_sku_id);
CREATE INDEX idx_variant_attributes_attr_id ON variant_attributes(attribute_id);
```

## Key Features

### Error Handling
- `DuplicateResourceException` - Prevents duplicate attributes/values
- `ResourceNotFoundException` - Entity not found scenarios
- `IllegalArgumentException` - Invalid relationships (e.g., value doesn't belong to attribute)

### Data Integrity
- Unique constraints at database level
- Service-level validation before operations
- Lazy loading with `@JsonIgnore` to prevent serialization issues
- Proper `@ToString.Exclude` on relations to prevent infinite recursion

### Transactional Safety
- All mutation operations wrapped in `@Transactional`
- Read-only operations marked with `@Transactional(readOnly = true)`
- Cascade delete from parent entities

## Usage Examples

### Create Attribute
```java
// Service call
ResponseErrorTemplate response = productAttributeService.createAttribute("Color");
```

### Assign Attribute to SKU
```java
// Get attribute ID (e.g., 1), attribute value ID (e.g., 5)
ResponseErrorTemplate response = variantAttributeService.assignAttributeToVariant(
    skuId,      // ProductSku ID
    attributeId, // ProductAttribute ID
    valueId     // ProductAttributeValue ID
);
```

### Get All Attributes for SKU
```java
List<ResponseErrorTemplate> attributes = variantAttributeService.getAttributesByVariantId(skuId);
```

## Integration Points

### Update ProductSku Entity (Optional)
To expose attributes directly from SKU:
```java
@OneToMany(mappedBy = "productSku", cascade = CascadeType.ALL, orphanRemoval = true)
@ToString.Exclude
private List<VariantAttribute> attributes = new ArrayList<>();
```

### Update ProductSkuResponse DTO (Optional)
```java
private List<VariantAttributeResponse> attributes;
```

## Notes
- All entities use Lombok annotations (`@Getter`, `@Setter`, `@Builder`, etc.)
- Using Jakarta Persistence API (JPA) with Spring Data
- `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` ensures only `id` is used for equality
- `@JsonIgnore` on foreign key relations prevents Jackson infinite recursion during serialization

