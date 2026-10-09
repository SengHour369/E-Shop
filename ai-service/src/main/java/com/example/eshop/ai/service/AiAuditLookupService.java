package com.example.eshop.ai.service;

import com.example.eshop.common.audit.AuditLog;
import com.example.eshop.common.audit.AuditLogRepository;
import com.example.eshop.common.security.LivePermissionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiAuditLookupService {

    private final AuditLogRepository audits;
    private final LivePermissionService permissions;
    private final ObjectMapper mapper;

    @Transactional(readOnly = true)
    public JsonNode find(String requestId) {
        permissions.require("AUDIT_VIEW");
        var page = PageRequest.of(0, 20, Sort.by("occurredAt").descending());
        var records = audits.findAll((root, query, builder) ->
                        builder.equal(root.get("requestId"), requestId), page)
                .stream()
                .map(AuditCard::from)
                .toList();
        return mapper.valueToTree(records);
    }

    public record AuditCard(Long id, String requestId, String traceId, String action,
            String resourceType, String resourceId, String result, String errorCode,
            java.time.Instant occurredAt) {

        static AuditCard from(AuditLog log) {
            return new AuditCard(log.getId(), log.getRequestId(), log.getTraceId(),
                    log.getAction().name(), log.getResourceType(), log.getResourceId(),
                    log.getResult().name(), log.getErrorCode(), log.getOccurredAt());
        }
    }
}
