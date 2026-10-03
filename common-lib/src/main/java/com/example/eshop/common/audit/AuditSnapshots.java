package com.example.eshop.common.audit;

import java.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class AuditSnapshots {
    // Allowlist: arbitrary fields cannot accidentally enter persistent audit JSON.
    private static final Set<String> ALLOWED = Set.of("name", "code", "sku", "id", "status", "active", "deleted", "price",
            "discountValue", "discountType", "promotionType", "maxDiscountAmount", "minimumOrderAmount",
            "startAt", "endAt", "priority", "usageLimit", "usagePerCustomer", "stackable", "skuId",
            "productId", "categoryId", "subCategoryId", "quantity", "reservedQuantity", "lowStockThreshold",
            "isDefault", "roleId", "permissionId", "groupId", "count",
            "intent", "toolName", "executionStatus", "serviceName", "aiExecutionId");
    private final ObjectMapper mapper;
    public AuditSnapshots(ObjectMapper mapper) { this.mapper = mapper; }
    public String json(Map<String, ?> values) {
        if (values == null) return null;
        if (values.size() > 64) throw new IllegalArgumentException("Audit snapshot has too many fields");
        Map<String, Object> safe = new TreeMap<>();
        values.forEach((key, value) -> {
            if (!ALLOWED.contains(key)) safe.put("redactedFields", "[REDACTED]");
            else if (value == null || value instanceof Number || value instanceof Boolean) safe.put(key, value);
            else if (value instanceof Enum<?> || value instanceof java.time.temporal.TemporalAccessor || value instanceof String)
                safe.put(key, AuditContextProvider.safeText(value.toString(), 256));
            else safe.put(key, "[REDACTED]");
        });
        try { return mapper.writeValueAsString(safe); }
        catch (JsonProcessingException ex) { throw new IllegalArgumentException("Invalid audit snapshot"); }
    }
}
