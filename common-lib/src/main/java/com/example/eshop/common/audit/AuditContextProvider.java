package com.example.eshop.common.audit;

import com.example.eshop.common.request.RequestIds;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuditContextProvider {
    private final String serviceName;
    public AuditContextProvider(@Value("${spring.application.name:unknown}") String serviceName) {
        this.serviceName = serviceName;
    }
    public record Context(String requestId, String traceId, String actorId, AuditActorType actorType,
            String ipAddress, String userAgent, String serviceName) {}
    public Context capture() { return capture(SecurityContextHolder.getContext().getAuthentication()); }
    public Context capture(Authentication auth) {
        var attributes = RequestContextHolder.getRequestAttributes();
        var request = attributes instanceof ServletRequestAttributes a ? a.getRequest() : null;
        String actorId = null;
        AuditActorType type = request == null ? AuditActorType.SYSTEM : AuditActorType.ANONYMOUS;
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            if (auth.getPrincipal() instanceof AuditPrincipal p && p.getUserId() != null)
                actorId = p.getUserId().toString();
            else if (auth.getDetails() instanceof Map<?, ?> claims && claims.get("userId") instanceof Number id)
                actorId = Long.toString(id.longValue());
            type = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))
                    ? AuditActorType.ADMIN : AuditActorType.USER;
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().startsWith("SERVICE_")))
                { type = AuditActorType.API_CLIENT; actorId = safeText(auth.getName(), 128); }
        }
        return new Context(RequestIds.current(), MDC.get("traceId"), actorId, type,
                request == null ? null : request.getRemoteAddr(),
                request == null ? null : safeText(request.getHeader("User-Agent"), 512), serviceName);
    }
    static String safeText(String value, int limit) {
        if (value == null) return null;
        String clean = value.replaceAll("[\\p{Cntrl}]", "");
        return clean.substring(0, Math.min(clean.length(), limit));
    }
}
