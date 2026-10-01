package com.example.eshop.catalog.audit;
import com.example.eshop.catalog.model.*;
import java.util.*;
/** Explicit scalars only: no entity serialization or lazy collections. */
public final class CatalogAuditSnapshots {
    private CatalogAuditSnapshots() {}
    private static Map<String,Object> fields(Object... fields) {
        Map<String,Object> result = new LinkedHashMap<>();
        for (int i = 0; i < fields.length; i += 2) result.put((String) fields[i], fields[i+1]);
        return result;
    }
    public static Map<String,Object> of(Promotion p) {
        return fields("id", p.getId(), "name", p.getName(), "status", p.getStatus(), "active", p.isActive(),
            "discountValue", p.getDiscountValue(), "discountType", p.getDiscountType(),
            "promotionType", p.getPromotionType(), "maxDiscountAmount", p.getMaxDiscountAmount(),
            "minimumOrderAmount", p.getMinimumOrderAmount(), "startAt", p.getStartAt(), "endAt", p.getEndAt(),
            "priority", p.getPriority(), "usageLimit", p.getUsageLimit(), "usagePerCustomer", p.getUsagePerCustomer(),
            "stackable", p.isStackable());
    }
    public static Map<String,Object> of(Product p) {
        return fields("id", p.getId(), "name", p.getName(), "active", p.getIsActive(), "deleted", p.getDeleted(),
            "subCategoryId", p.getSubCategory() == null ? null : p.getSubCategory().getId());
    }
    public static Map<String,Object> of(ProductSku p) {
        return fields("id", p.getId(), "sku", p.getSku(), "productId", p.getProduct().getId(), "price", p.getPrice(), "isDefault", p.getIsDefault());
    }
    public static Map<String,Object> of(Category p) {
        return fields("id", p.getId(), "name", p.getName(), "status", p.getStatus(), "deleted", p.getDeleted());
    }
}
