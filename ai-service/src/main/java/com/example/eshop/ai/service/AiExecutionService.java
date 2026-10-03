package com.example.eshop.ai.service;
import com.example.eshop.ai.model.*;
import com.example.eshop.ai.repository.*;
import com.example.eshop.ai.registry.AiToolDefinition;
import com.example.eshop.ai.enums.*;
import com.example.eshop.common.audit.*;
import com.example.eshop.common.notification.NotificationEvent;
import com.example.eshop.common.request.RequestIds;
import com.example.eshop.common.security.CurrentActor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.time.Instant;
import java.util.*;
@Service @RequiredArgsConstructor
public class AiExecutionService {
    private final AiExecutionRepository executions;
    private final NotificationOutboxRepository outbox;
    private final AuditLogService audit;
    private final ObjectMapper mapper;
    @Transactional(readOnly=true)
    public Optional<AiExecution> existing(UUID id) {
        var result = executions.findById(id);
        result.ifPresent(e -> { if (e.getUserId() != CurrentActor.userId()) throw new AccessDeniedException("Execution unavailable"); });
        return result;
    }
    @Transactional
    public AiExecution start(UUID id) {
        var e = new AiExecution();
        e.setId(id); e.setUserId(CurrentActor.userId()); e.setRequestId(RequestIds.current());
        e.setTraceId(org.slf4j.MDC.get("traceId")); e.setStartedAt(Instant.now()); e.setStatus(AiExecutionStatus.RUNNING);
        executions.saveAndFlush(e);
        audit.record(new AuditEvent(AuditAction.AI_REQUEST_ACCEPTED, "AI_EXECUTION", id.toString(), null,
            Map.of("executionStatus", "RUNNING", "aiExecutionId", id.toString()), AuditResult.SUCCESS, null));
        return e;
    }
    @Transactional
    public void finish(UUID id, AiIntent intent, AiToolDefinition tool, AiExecutionStatus status, String code, String resourceId) {
        var e = executions.findById(id).orElseThrow();
        if (e.getUserId() != CurrentActor.userId() || e.getStatus() != AiExecutionStatus.RUNNING) throw new AccessDeniedException("Execution unavailable");
        e.setIntent(intent); e.setStatus(status); e.setErrorCode(code); e.setResourceId(resourceId); e.setCompletedAt(Instant.now());
        String resource = intent == AiIntent.PROMOTION_CREATE ? "PROMOTION" : "AI_EXECUTION";
        var metadata = new LinkedHashMap<String,Object>();
        metadata.put("intent", intent); metadata.put("toolName", intent); metadata.put("executionStatus", status);
        metadata.put("serviceName", tool == null ? "ai-service" : tool.service()); metadata.put("aiExecutionId", id.toString());
        audit.record(new AuditEvent(AuditAction.AI_TOOL_EXECUTION, resource, resourceId == null ? id.toString() : resourceId,
            null, metadata, status == AiExecutionStatus.SUCCESS ? AuditResult.SUCCESS : status == AiExecutionStatus.DENIED ? AuditResult.DENIED : AuditResult.FAILURE, code));
        if (tool != null && tool.notifyOutcome() && (status == AiExecutionStatus.SUCCESS || status == AiExecutionStatus.FAILURE || status == AiExecutionStatus.UNKNOWN)) {
            var event = new NotificationEvent(1, UUID.randomUUID(), e.getRequestId(), e.getTraceId(), org.slf4j.MDC.get("spanId"),
                id, e.getUserId(), status == AiExecutionStatus.SUCCESS ? NotificationEvent.Type.AI_ACTION_COMPLETED : NotificationEvent.Type.AI_ACTION_FAILED,
                resource, resourceId, Instant.now());
            event.validate();
            var row = new NotificationOutbox(); row.setId(event.eventId()); row.setCreatedAt(Instant.now()); row.setNextAttemptAt(Instant.now());
            try { row.setPayload(mapper.writeValueAsString(event)); }
            catch (Exception ex) { throw new IllegalStateException("Cannot serialize notification event"); }
            outbox.save(row);
        }
    }
}
