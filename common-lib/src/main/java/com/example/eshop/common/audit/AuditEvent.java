package com.example.eshop.common.audit;

import java.util.Map;

/** Snapshots must contain explicitly selected scalar fields, never entities or request bodies. */
public record AuditEvent(AuditAction action, String resourceType, String resourceId,
        Map<String, ?> oldValue, Map<String, ?> newValue, AuditResult result, String errorCode) {
    public static AuditEvent success(AuditAction action, String type, Object id,
            Map<String, ?> before, Map<String, ?> after) {
        return new AuditEvent(action, type, id == null ? null : id.toString(), before, after, AuditResult.SUCCESS, null);
    }
}
