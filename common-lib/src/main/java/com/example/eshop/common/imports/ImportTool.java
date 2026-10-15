package com.example.eshop.common.imports;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/** Fixed handlers and schemas shared by staging and the authoritative domain boundary. */
public enum ImportTool {
    CATEGORY_BULK_CREATE("CATEGORY_CREATE", false, true, "name:text! description:text status:boolean!"),
    BRANCH_BULK_CREATE("BRANCH_CREATE", true, true, "name:text! address:text"),
    PRODUCT_BULK_CREATE("PRODUCT_CREATE", true, true, "name:text! subCategoryId:id! description:text isActive:boolean! imageIds:text imageChecksums:text"),
    PRODUCT_SKU_BULK_CREATE("SKU_CREATE", false, true, "productId:id! description:text! price:money! quantity:zero! barcode:text"),
    INVENTORY_STOCK_IN_BULK("INVENTORY_ADJUST", true, true, "inventoryId:id! quantity:id! branchId:id"),
    INVENTORY_STOCK_ADJUST_BULK("INVENTORY_ADJUST", true, true, "inventoryId:id! quantity:zero! branchId:id"),
    PROMOTION_BULK_CREATE("PROMOTION_MANAGE", true, true, "skuId:id! name:text! discount:percent! startAt:date! endAt:date!"),
    IMPORT_VALIDATE("IMPORT_UPLOAD", false, true, ""),
    IMPORT_PREVIEW("IMPORT_UPLOAD", false, true, ""),
    IMPORT_SUBMIT("IMPORT_UPLOAD", false, true, ""),
    IMPORT_APPROVE("IMPORT_APPROVE", true, true, ""),
    IMPORT_REJECT("IMPORT_REVIEW", false, true, ""),
    IMPORT_REQUEST_CHANGES("IMPORT_REVIEW", false, true, ""),
    IMPORT_EXECUTION_STATUS("IMPORT_UPLOAD", false, true, "");

    public final String permission;
    public final boolean highRisk;
    public final boolean enabled;
    public final Map<String, Field> schema;
    public record Field(String type, boolean required) {}
    ImportTool(String permission, boolean highRisk, boolean enabled, String schema) {
        this.permission = permission; this.highRisk = highRisk; this.enabled = enabled;
        Map<String, Field> fields = new LinkedHashMap<>();
        if (!schema.isBlank()) for (String spec : schema.split(" ")) {
            String[] pair = spec.split(":");
            fields.put(pair[0], new Field(pair[1].replace("!", ""), pair[1].endsWith("!")));
        }
        this.schema = Collections.unmodifiableMap(fields);
    }
    public boolean domainTool() { return !schema.isEmpty(); }
    public String unavailableReason() {
        return "";
    }
    public List<String> validate(Map<String, String> values) {
        List<String> errors = new ArrayList<>();
        if (!enabled || !domainTool()) errors.add("UNSUPPORTED_IMPORT: " + unavailableReason());
        if (values == null) return List.of("INVALID_ROW");
        for (String key : values.keySet()) if (!schema.containsKey(key)) errors.add("UNKNOWN_FIELD: " + key);
        schema.forEach((key, field) -> {
            String value = values.get(key);
            if (value == null || value.isBlank()) {
                if (field.required()) errors.add("REQUIRED: " + key);
                return;
            }
            try {
                if (value.length() > 2000 || value.chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException();
                switch (field.type()) {
                    case "id", "zero" -> { long n = Long.parseLong(value); if (n < (field.type().equals("id") ? 1 : 0) || n > 1_000_000_000L) throw new IllegalArgumentException(); }
                    case "money", "percent" -> { BigDecimal n = new BigDecimal(value); if (n.signum() <= 0 || n.scale() > 4 || n.precision() > 15 || field.type().equals("percent") && n.compareTo(new BigDecimal("100")) > 0) throw new IllegalArgumentException(); }
                    case "boolean" -> { if (!Set.of("true", "false").contains(value)) throw new IllegalArgumentException(); }
                    case "date" -> LocalDateTime.parse(value);
                    default -> { if (key.equals("name") && value.length() > 120 || key.equals("barcode") && value.length() > 64 || key.equals("address") && value.length() > 500) throw new IllegalArgumentException(); }
                }
            } catch (RuntimeException ex) { errors.add("INVALID_VALUE: " + key); }
        });
        if (this == PROMOTION_BULK_CREATE && errors.isEmpty()
                && !LocalDateTime.parse(values.get("startAt")).isBefore(LocalDateTime.parse(values.get("endAt")))) errors.add("INVALID_DATE_RANGE");
        if (this == PRODUCT_BULK_CREATE) {
            String ids=values.getOrDefault("imageIds",""), hashes=values.getOrDefault("imageChecksums","");
            if(ids.isBlank()||hashes.isBlank()) errors.add("IMAGE_ATTACHMENT_REQUIRED");
            else if(!ids.matches("[a-f0-9-]{36}(,[a-f0-9-]{36}){0,3}")||!hashes.matches("[a-f0-9]{64}(,[a-f0-9]{64}){0,3}")||ids.split(",").length!=hashes.split(",").length)errors.add("INVALID_IMAGE_REFERENCES");
        }
        return errors;
    }
}
