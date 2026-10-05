package com.example.eshop.ai.service;

import com.example.eshop.ai.enums.AiExecutionStatus;
import com.example.eshop.ai.enums.AiIntent;
import com.example.eshop.ai.model.AiExecution;
import com.example.eshop.ai.model.NotificationOutbox;
import com.example.eshop.ai.registry.AiToolDefinition;
import com.example.eshop.ai.repository.AiExecutionRepository;
import com.example.eshop.ai.repository.NotificationOutboxRepository;
import com.example.eshop.common.audit.AuditAction;
import com.example.eshop.common.audit.AuditEvent;
import com.example.eshop.common.audit.AuditLogService;
import com.example.eshop.common.audit.AuditResult;
import com.example.eshop.common.notification.NotificationEvent;
import com.example.eshop.common.request.RequestIds;
import com.example.eshop.common.security.CurrentActor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Owns the execution row, its audit record, and the notification outbox for one request. */
@Service
@RequiredArgsConstructor
public class AiExecutionService {

    private final AiExecutionRepository executions;
    private final NotificationOutboxRepository outbox;
    private final AuditLogService audit;
    private final ObjectMapper mapper;

    @Transactional(readOnly = true)
    public Optional<AiExecution> existing(UUID id) {
        Optional<AiExecution> result = executions.findById(id);
        result.ifPresent(this::requireOwner);
        return result;
    }

    @Transactional
    public AiExecution start(UUID id) {
        AiExecution execution = new AiExecution();
        execution.setId(id);
        execution.setUserId(CurrentActor.userId());
        execution.setRequestId(RequestIds.current());
        execution.setTraceId(MDC.get("traceId"));
        execution.setStartedAt(Instant.now());
        execution.setStatus(AiExecutionStatus.RUNNING);
        executions.saveAndFlush(execution);
        audit.record(new AuditEvent(
                AuditAction.AI_REQUEST_ACCEPTED,
                "AI_EXECUTION",
                id.toString(),
                null,
                Map.of("executionStatus", "RUNNING", "aiExecutionId", id.toString()),
                AuditResult.SUCCESS,
                null));
        return execution;
    }

    @Transactional
    public void finish(UUID id, AiIntent intent, AiToolDefinition tool, AiExecutionStatus status,
            String code, String resourceId) {
        AiExecution execution = executions.findById(id).orElseThrow();
        if (execution.getUserId() != CurrentActor.userId() || execution.getStatus() != AiExecutionStatus.RUNNING) {
            throw new AccessDeniedException("Execution unavailable");
        }
        execution.setIntent(intent);
        execution.setStatus(status);
        execution.setErrorCode(code);
        execution.setResourceId(resourceId);
        execution.setCompletedAt(Instant.now());

        String resource = intent == AiIntent.PROMOTION_CREATE ? "PROMOTION" : "AI_EXECUTION";
        auditOutcome(id, intent, tool, status, code, resource, resourceId);
        if (shouldNotify(tool, status)) {
            enqueue(execution, status, resource, resourceId);
        }
    }

    private void requireOwner(AiExecution execution) {
        if (execution.getUserId() != CurrentActor.userId()) {
            throw new AccessDeniedException("Execution unavailable");
        }
    }

    private void auditOutcome(UUID id, AiIntent intent, AiToolDefinition tool, AiExecutionStatus status,
            String code, String resource, String resourceId) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("intent", intent);
        metadata.put("toolName", intent);
        metadata.put("executionStatus", status);
        metadata.put("serviceName", tool == null ? "ai-service" : tool.service());
        metadata.put("aiExecutionId", id.toString());
        audit.record(new AuditEvent(
                AuditAction.AI_TOOL_EXECUTION,
                resource,
                resourceId == null ? id.toString() : resourceId,
                null,
                metadata,
                auditResult(status),
                code));
    }

    private static AuditResult auditResult(AiExecutionStatus status) {
        if (status == AiExecutionStatus.SUCCESS) {
            return AuditResult.SUCCESS;
        }
        if (status == AiExecutionStatus.DENIED) {
            return AuditResult.DENIED;
        }
        return AuditResult.FAILURE;
    }

    private static boolean shouldNotify(AiToolDefinition tool, AiExecutionStatus status) {
        return tool != null
                && tool.notifyOutcome()
                && (status == AiExecutionStatus.SUCCESS
                || status == AiExecutionStatus.FAILURE
                || status == AiExecutionStatus.UNKNOWN);
    }

    private void enqueue(AiExecution execution, AiExecutionStatus status, String resource, String resourceId) {
        NotificationEvent.Type type = status == AiExecutionStatus.SUCCESS
                ? NotificationEvent.Type.AI_ACTION_COMPLETED
                : NotificationEvent.Type.AI_ACTION_FAILED;
        NotificationEvent event = new NotificationEvent(
                1,
                UUID.randomUUID(),
                execution.getRequestId(),
                execution.getTraceId(),
                MDC.get("spanId"),
                execution.getId(),
                execution.getUserId(),
                type,
                resource,
                resourceId,
                Instant.now());
        event.validate();

        NotificationOutbox row = new NotificationOutbox();
        row.setId(event.eventId());
        row.setCreatedAt(Instant.now());
        row.setNextAttemptAt(Instant.now());
        try {
            row.setPayload(mapper.writeValueAsString(event));
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot serialize notification event");
        }
        outbox.save(row);
    }
}
