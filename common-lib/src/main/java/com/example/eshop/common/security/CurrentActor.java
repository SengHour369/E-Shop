package com.example.eshop.common.security;

import com.example.eshop.common.audit.AuditPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Map;

/** Only identities established by the security filter are authoritative. */
public final class CurrentActor {
    private CurrentActor() {}
    public static long userId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            if (auth.getPrincipal() instanceof AuditPrincipal principal && principal.getUserId() != null)
                return principal.getUserId();
            if (auth.getDetails() instanceof Map<?, ?> claims && claims.get("userId") instanceof Number n && n.longValue() > 0)
                return n.longValue();
        }
        throw new AccessDeniedException("An authenticated customer identity is required");
    }
    public static boolean has(String authority) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }
}
