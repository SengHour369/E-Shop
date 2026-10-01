package com.example.eshop.common.security;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import java.util.Map;
public final class CurrentCustomer {
    private CurrentCustomer() {}
    public static void require(Long userId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getDetails() instanceof Map<?, ?> claims)
                || !(claims.get("userId") instanceof Number id) || !Long.valueOf(id.longValue()).equals(userId))
            throw new AccessDeniedException("Customer identity mismatch; sign in again if your token predates checkout");
    }
}

