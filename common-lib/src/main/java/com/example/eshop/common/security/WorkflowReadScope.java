package com.example.eshop.common.security;

/** Delegated workflow credentials cannot be used for ordinary admin or customer mutations. */
public final class WorkflowReadScope {
    private WorkflowReadScope() {}
    public static boolean allows(String method,String path) {
        if (!"GET".equals(method)) return false;
        return path.equals("/internal/authorization/me") || path.equals("/api/notifications")
                || path.matches("/internal/ai/(products|skus|inventory|promotions)(/[A-Za-z0-9_-]+)?")
                || path.matches("/internal/ai/(orders|payments|returns|users)(/[^/]+)?");
    }
}
