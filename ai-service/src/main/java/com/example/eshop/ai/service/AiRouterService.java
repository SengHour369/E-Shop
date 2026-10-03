package com.example.eshop.ai.service;
import com.example.eshop.ai.client.AiProviderClient;
import com.example.eshop.ai.dto.*;
import com.example.eshop.ai.registry.*;
import com.example.eshop.ai.enums.*;
import com.example.eshop.ai.model.AiExecution;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import feign.FeignException;
import java.util.UUID;
import static com.example.eshop.ai.enums.AiExecutionStatus.*;
@Service @RequiredArgsConstructor
public class AiRouterService {
    private final AiProviderClient provider;
    private final AiToolRegistry registry;
    private final AiToolExecutor executor;
    private final AiExecutionService history;
    public AiResponse execute(AiRequest request, UUID id, String bearer) {
        var previous = history.existing(id);
        if (previous.isPresent()) return replay(previous.get());
        AiExecution execution;
        try { execution = history.start(id); }
        catch (DataIntegrityViolationException e) { return replay(history.existing(id).orElseThrow()); }
        AiIntent intent = AiIntent.UNKNOWN;
        AiToolDefinition tool = null;
        AiExecutionStatus status = FAILURE;
        String code = null, message = "Operation completed.", resourceId = null;
        JsonNode data = null;
        boolean dispatched = false;
        try {
            var result = provider.detect(request.message(), registry.available());
            if (result == null || result.intent() == null || !Double.isFinite(result.confidence()) || result.confidence() < 0.8 || result.confidence() > 1)
                throw new AiFailure("UNCERTAIN_INTENT", NEEDS_INPUT, "Please clarify the operation and its parameters.");
            intent = result.intent();
            tool = registry.authorize(intent);
            registry.validate(tool, result.parameters());
            dispatched = true;
            data = executor.execute(tool, result.parameters(), bearer);
            if (data == null || data.has("code") && !"200".equals(data.path("code").asText()) && !"201".equals(data.path("code").asText()) || data.path("status").isInt() && data.path("status").asInt() >= 400
                    || data.path("errorCode").isInt() && data.path("errorCode").asInt() != 0)
                throw new AiFailure("DOWNSTREAM_REJECTED", FAILURE, "The operation was rejected by the service.");
            status = SUCCESS;
            if (intent == AiIntent.PROMOTION_CREATE && data.path("id").isIntegralNumber()) resourceId = data.path("id").asText();
        } catch (AiFailure e) { status = e.status(); code = e.code(); message = e.getMessage(); }
        catch (FeignException e) {
            boolean uncertainWrite = dispatched && tool != null && tool.risk() != AiToolRisk.READ_ONLY && (e.status() < 0 || e.status() >= 500);
            status = uncertainWrite ? UNKNOWN : e.status() == 403 || e.status() == 401 ? DENIED : FAILURE;
            code = uncertainWrite ? "OUTCOME_UNKNOWN" : "DOWNSTREAM_FAILURE";
            message = uncertainWrite ? "The service outcome is uncertain. Check execution history and promotions before submitting another request." : "The service could not complete the operation.";
        } catch (Exception e) {
            status = dispatched && tool != null && tool.risk() != AiToolRisk.READ_ONLY ? UNKNOWN : FAILURE;
            code = "EXECUTION_FAILURE"; message = "The operation could not be completed safely.";
        }
        history.finish(id, intent, tool, status, code, resourceId);
        return new AiResponse(id, execution.getRequestId(), execution.getTraceId(), intent, status, message, code, status == SUCCESS ? data : null);
    }
    private AiResponse replay(AiExecution e) {
        return new AiResponse(e.getId(), e.getRequestId(), e.getTraceId(), e.getIntent(), e.getStatus(),
            "This request was already accepted; it has not been executed again. RUNNING executions may require reconciliation after an interrupted request.",
            e.getErrorCode(), null);
    }
}
