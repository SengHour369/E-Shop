package com.example.eshop.catalog.mapper;

import com.example.eshop.catalog.model.*;
import java.util.List;
import java.util.Map;

/** Request-scoped lookup tables; inventory is never cached across requests. */
public record ProductDetails(Map<Long, List<ProductSku>> skus,
                             Map<Long, List<ProductAttribute>> attributes,
                             Map<Long, List<ProductAttributeValue>> values,
                             Map<Long, Inventory> inventories,
                             Map<Long, com.example.eshop.catalog.dto.response.PriceResult> prices) {}
