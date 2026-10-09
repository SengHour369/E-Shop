package com.example.eshop.ai.service;

import com.example.eshop.ai.client.AiProviderClient;
import com.example.eshop.ai.dto.AiIntentResult;
import com.example.eshop.ai.dto.AiRequest;
import com.example.eshop.ai.dto.AiResponse;
import com.example.eshop.ai.enums.AiExecutionStatus;
import com.example.eshop.ai.enums.AiIntent;
import com.example.eshop.ai.enums.AiToolRisk;
import com.example.eshop.ai.model.AiExecution;
import com.example.eshop.ai.registry.AiToolDefinition;
import com.example.eshop.ai.registry.AiToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

import static com.example.eshop.ai.enums.AiExecutionStatus.DENIED;
import static com.example.eshop.ai.enums.AiExecutionStatus.FAILURE;
import static com.example.eshop.ai.enums.AiExecutionStatus.NEEDS_INPUT;
import static com.example.eshop.ai.enums.AiExecutionStatus.SUCCESS;
import static com.example.eshop.ai.enums.AiExecutionStatus.UNKNOWN;

/**
 * One request follows a fixed path: replay an accepted key, detect an intent,
 * authorize and validate it, then call the registered client. The model never
 * chooses a URL, and a replay does not run the tool again.
 */
@Service
@RequiredArgsConstructor
public class AiRouterService {

    private static final double MIN_CONFIDENCE = 0.8;

    private final AiProviderClient provider;
    private final AiToolRegistry registry;
    private final AiToolExecutor executor;
    private final AiExecutionService history;
    private final AiConfirmationService confirmations;
    private final io.micrometer.core.instrument.MeterRegistry metrics;

    public AiResponse confirm(UUID id, String bearer) {
        var previous = history.existing(id);
        if (previous.isPresent()) {
            return replay(previous.get());
        }
        AiExecution execution;
        try {
            execution = history.start(id);
        } catch (DataIntegrityViolationException exception) {
            return replay(history.existing(id).orElseThrow());
        }
        Outcome outcome;
        try {
            var result = executor.executeConfirmed(id, bearer);
            if (rejected(result.data())) {
                outcome = Outcome.of(result.tool().toolName(), result.tool(), FAILURE,
                        "DOWNSTREAM_REJECTED", "The service rejected the action.");
            } else {
                outcome = Outcome.success(result.tool().toolName(), result.tool(), result.data(),
                        promotionId(result.tool().toolName(), result.data()));
            }
        } catch (AiFailure exception) {
            outcome = Outcome.of(AiIntent.UNKNOWN, null, exception.status(), exception.code(), exception.getMessage());
        } catch (org.springframework.security.access.AccessDeniedException exception) {
            outcome = Outcome.of(AiIntent.UNKNOWN, null, DENIED, "PERMISSION_DENIED", "This action is not authorized.");
        } catch (Exception exception) {
            outcome = Outcome.of(AiIntent.UNKNOWN, null, UNKNOWN, "OUTCOME_UNKNOWN",
                    "The action could not be confirmed. Check its state before submitting it again.");
        }
        history.finish(id, outcome.intent(), outcome.tool(), outcome.status(), outcome.code(), outcome.resourceId());
        return response(id, execution, outcome);
    }

    public AiResponse execute(AiRequest request, UUID id, String bearer) {
        if (!AiToolRegistry.authenticated()) {
            Outcome outcome = run(request, null);
            history.recordGuest(id, outcome.intent(), outcome.status(), outcome.code());
            return new AiResponse(id, com.example.eshop.common.request.RequestIds.current(),
                    org.slf4j.MDC.get("traceId"), outcome.intent(), outcome.status(),
                    outcome.message(), outcome.code(), outcome.data());
        }
        Optional<AiExecution> previous = history.existing(id);
        if (previous.isPresent()) {
            return replay(previous.get());
        }

        AiExecution execution;
        try {
            execution = history.start(id);
        } catch (DataIntegrityViolationException ex) {
            return replay(history.existing(id).orElseThrow());
        }

        Outcome outcome = run(request, bearer);
        history.finish(id, outcome.intent(), outcome.tool(), outcome.status(), outcome.code(), outcome.resourceId());
        return response(id, execution, outcome);
    }

    private Outcome run(AiRequest request, String bearer) {
        long started = System.nanoTime();
        Outcome outcome = evaluate(request, bearer);
        metrics.timer("ai.request.duration", "intent", outcome.intent().name(), "status", outcome.status().name())
                .record(System.nanoTime() - started, java.util.concurrent.TimeUnit.NANOSECONDS);
        metrics.counter("ai.requests", "status", outcome.status().name()).increment();
        return outcome;
    }

    private Outcome evaluate(AiRequest request, String bearer) {
        AiIntent intent = AiIntent.UNKNOWN;
        AiToolDefinition tool = null;
        boolean dispatched = false;
        try {
            AiIntentResult detected = provider.detect(request.message(), registry.available());
            requireConfident(detected);
            intent = detected.intent();
            tool = registry.authorize(intent);
            registry.validate(tool, detected.parameters());
            if (tool.risk() != AiToolRisk.READ_ONLY) {
                JsonNode preview = executor.preview(tool, detected.parameters(), bearer);
                JsonNode confirmation = confirmations.prepare(intent, detected.parameters());
                if (preview != null && confirmation instanceof com.fasterxml.jackson.databind.node.ObjectNode card) {
                    card.set("preview", preview);
                }
                return new Outcome(intent, tool, NEEDS_INPUT, "CONFIRMATION_REQUIRED",
                        "Review the proposed action and confirm within five minutes.", null, confirmation);
            }
            dispatched = true;
            JsonNode data = executor.execute(tool, detected.parameters(), bearer);
            if (rejected(data)) {
                throw new AiFailure("DOWNSTREAM_REJECTED", FAILURE, "The operation was rejected by the service.");
            }
            return Outcome.success(intent, tool, data, promotionId(intent, data));
        } catch (AiFailure ex) {
            return Outcome.of(intent, tool, ex.status(), ex.code(), ex.getMessage());
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return Outcome.of(intent, tool, DENIED, "PERMISSION_DENIED", "This operation is not authorized.");
        } catch (FeignException ex) {
            return feignOutcome(intent, tool, dispatched, ex);
        } catch (Exception ex) {
            return unexpectedOutcome(intent, tool, dispatched);
        }
    }

    private static void requireConfident(AiIntentResult result) {
        if (result == null
                || result.intent() == null
                || !Double.isFinite(result.confidence())
                || result.confidence() < MIN_CONFIDENCE
                || result.confidence() > 1) {
            throw new AiFailure("UNCERTAIN_INTENT", NEEDS_INPUT, "Please clarify the operation and its parameters.");
        }
    }

    /** A body is rejected when it is missing or carries a non-success code, HTTP status, or error code. */
    private static boolean rejected(JsonNode data) {
        if (data == null) {
            return true;
        }
        if (data.has("code")) {
            String code = data.path("code").asText();
            if (!"200".equals(code) && !"201".equals(code)) {
                return true;
            }
        }
        if (data.path("status").isInt() && data.path("status").asInt() >= 400) {
            return true;
        }
        return data.path("errorCode").isInt() && data.path("errorCode").asInt() != 0;
    }

    private static String promotionId(AiIntent intent, JsonNode data) {
        if ((intent == AiIntent.PROMOTION_CREATE || intent == AiIntent.PROMOTION_DISABLE || intent == AiIntent.ORDER_CANCEL)
                && data.path("id").isIntegralNumber()) {
            return data.path("id").asText();
        }
        return null;
    }

    private static Outcome feignOutcome(AiIntent intent, AiToolDefinition tool, boolean dispatched, FeignException ex) {
        if (uncertainWrite(dispatched, tool) && (ex.status() < 0 || ex.status() >= 500)) {
            return Outcome.of(intent, tool, UNKNOWN, "OUTCOME_UNKNOWN",
                    "The service outcome is uncertain. Check execution history and promotions before submitting another request.");
        }
        AiExecutionStatus status = ex.status() == 401 || ex.status() == 403 ? DENIED : FAILURE;
        return Outcome.of(intent, tool, status, "DOWNSTREAM_FAILURE", "The service could not complete the operation.");
    }

    private static Outcome unexpectedOutcome(AiIntent intent, AiToolDefinition tool, boolean dispatched) {
        AiExecutionStatus status = uncertainWrite(dispatched, tool) ? UNKNOWN : FAILURE;
        return Outcome.of(intent, tool, status, "EXECUTION_FAILURE", "The operation could not be completed safely.");
    }

    private static boolean uncertainWrite(boolean dispatched, AiToolDefinition tool) {
        return dispatched && tool != null && tool.risk() != AiToolRisk.READ_ONLY;
    }

    private static AiResponse response(UUID id, AiExecution execution, Outcome outcome) {
        JsonNode data = outcome.status() == SUCCESS || outcome.status() == NEEDS_INPUT ? outcome.data() : null;
        return new AiResponse(id, execution.getRequestId(), execution.getTraceId(),
                outcome.intent(), outcome.status(), outcome.message(), outcome.code(), data);
    }

    private AiResponse replay(AiExecution execution) {
        return new AiResponse(execution.getId(), execution.getRequestId(), execution.getTraceId(),
                execution.getIntent(), execution.getStatus(),
                "This request was already accepted; it has not been executed again. RUNNING executions may require reconciliation after an interrupted request.",
                execution.getErrorCode(), null);
    }

    private record Outcome(AiIntent intent, AiToolDefinition tool, AiExecutionStatus status,
            String code, String message, String resourceId, JsonNode data) {

        static Outcome success(AiIntent intent, AiToolDefinition tool, JsonNode data, String resourceId) {
            return new Outcome(intent, tool, SUCCESS, null, "Operation completed.", resourceId, data);
        }

        static Outcome of(AiIntent intent, AiToolDefinition tool, AiExecutionStatus status, String code, String message) {
            return new Outcome(intent, tool, status, code, message, null, null);
        }
    }
}
