package com.example.eshop.common.audit;

import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

public record AuditSearch(String requestId, String traceId, String actorId, AuditActorType actorType,
        AuditAction action, String resourceType, String resourceId, AuditResult result, String serviceName,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
    public Specification<AuditLog> specification() {
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            Map<String, Object> filters = new LinkedHashMap<>();
            filters.put("requestId", requestId); filters.put("traceId", traceId); filters.put("actorId", actorId);
            filters.put("actorType", actorType); filters.put("action", action); filters.put("resourceType", resourceType);
            filters.put("resourceId", resourceId); filters.put("result", result); filters.put("serviceName", serviceName);
            filters.forEach((key, value) -> { if (value != null) predicates.add(cb.equal(root.get(key), value)); });
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
