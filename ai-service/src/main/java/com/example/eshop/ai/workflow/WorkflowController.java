package com.example.eshop.ai.workflow;

import com.example.eshop.ai.enums.AiIntent;
import com.example.eshop.ai.registry.AiToolRegistry;
import com.example.eshop.common.security.LivePermissionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

/** Draft authoring only. Validation deliberately never invokes a business tool. */
@RestController
@RequestMapping("/api/ai/admin/workflows")
public class WorkflowController {

    private final WorkflowDraftRepository drafts;
    private final LivePermissionService permissions;
    private final AiToolRegistry registry;
    private final ObjectMapper mapper;

    public WorkflowController(
            WorkflowDraftRepository drafts,
            LivePermissionService permissions,
            AiToolRegistry registry,
            ObjectMapper mapper
    ) {
        this.drafts = drafts;
        this.permissions = permissions;
        this.registry = registry;
        this.mapper = mapper;
    }

    private long actor() {
        var actor = permissions.current();

        if (!actor.administrator()) {
            throw new AccessDeniedException("Administrator access required");
        }

        return actor.userId();
    }

    public record Write(Long revision, JsonNode definition) {}

    public record Draft(
            UUID id,
            Long revision,
            String status,
            JsonNode definition,
            Instant updatedAt
    ) {}

    private Draft view(WorkflowDraft draft) {
        try {
            return new Draft(
                    draft.getId(),
                    draft.getRevision(),
                    "DRAFT",
                    mapper.readTree(draft.getDefinition()),
                    draft.getUpdatedAt()
            );
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to read saved draft"
            );
        }
    }

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String search
    ) {
        long owner = actor();

        if (page < 0 || search.length() > 120) {
            throw bad("Invalid filters");
        }

        var rows = drafts.findByOwnerIdAndNameContainingIgnoreCase(
                owner,
                search.trim(),
                PageRequest.of(
                        page,
                        20,
                        Sort.by("updatedAt").descending()
                )
        );
        return Map.of(
                "items", rows.getContent().stream()
                        .map(this::view)
                        .toList(),
                "total", rows.getTotalElements()
        );
    }

    @GetMapping("/catalog")
    public Map<String, Object> catalog() {
        actor();
        var tools = new java.util.ArrayList<Object>(
                registry.available().stream()
                        .map(t -> Map.of(
                                "code", t.toolName().name(),
                                "description", t.description(),
                                "module", t.service(),
                                "risk", t.risk().name(),
                                "parameters", t.requestSchema()
                        ))
                        .toList()
        );
        tools.add(Map.of(
                "code", "WORKFLOW_CONDITION",
                "description", "Branch on a real earlier step response",
                "module", "workflow-engine",
                "risk", "READ_ONLY",
                "parameters", Map.of()
        ));
        tools.add(Map.of(
                "code", "WORKFLOW_DELAY",
                "description", "Wait up to 30 seconds during a manual run",
                "module", "workflow-engine",
                "risk", "READ_ONLY",
                "parameters", Map.of()
        ));
        tools.add(Map.of(
                "code", "WORKFLOW_REPEAT",
                "description", "Repeat a permitted read tool up to 10 times",
                "module", "workflow-engine",
                "risk", "READ_ONLY",
                "parameters", Map.of()
        ));
        tools.add(Map.of(
                "code", "WORKFLOW_END",
                "description", "Finish this execution branch",
                "module", "workflow-engine",
                "risk", "READ_ONLY",
                "parameters", Map.of()
        ));
        return Map.of(
                "tools", tools,
                "triggerTypes", List.of("MANUAL"),
                "executionAvailable", false,
                "manualReadOnlyExecutionAvailable", true,
                "executableToolCodes", java.util.stream.Stream.concat(
                        WorkflowReadPolicy.ALLOWED.stream().map(Enum::name),
                        WorkflowControlPolicy.CODES.stream()
                ).sorted().toList(),
                "limitation",
                "Drafts, immutable read-only publication and manual "
                        + "catalog/inventory executions are available through "
                        + "the backend API. Forward-only conditions, end steps, "
                        + "bounded manual delays and repeated reads are "
                        + "supported. Write tools, events, scheduling and "
                        + "approval execution are not supported. Delays run "
                        + "within the current manual request; they are not "
                        + "durable scheduled jobs. Only the listed executable "
                        + "tools can be published and run; unsupported controls "
                        + "are unavailable."
        );
    }

    @GetMapping("/{id}")
    public Draft get(@PathVariable UUID id) {
        return view(owned(id, actor()));
    }

    @PostMapping
    @Transactional
    public Draft create(@RequestBody Write body) {
        long owner = actor();
        checkDraft(body.definition());
        var draft = new WorkflowDraft();
        draft.setId(UUID.randomUUID());
        draft.setOwnerId(owner);
        apply(draft, body.definition());
        return view(drafts.saveAndFlush(draft));
    }

    @PutMapping("/{id}")
    @Transactional
    public Draft update(@PathVariable UUID id, @RequestBody Write body) {
        var draft = owned(id, actor());

        if (!Objects.equals(draft.getRevision(), body.revision())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Draft changed. Reload before saving."
            );
        }

        checkDraft(body.definition());
        apply(draft, body.definition());
        return view(drafts.saveAndFlush(draft));
    }

    @PostMapping("/validate")
    public Map<String, Object> validate(@RequestBody Write body) {
        actor();
        checkDraft(body.definition());
        var definition = body.definition();
        var errors = new ArrayList<String>();
        var steps = new ArrayList<Map<String, Object>>();

        if (definition.path("description").asText().isBlank()) {
            errors.add("Description is required");
        }

        if (!definition.path("module").asText().equals("workflow-engine")
                && registry.available().stream().noneMatch(t ->
                t.service().equals(
                        definition.path("module").asText()
                ))) {
            errors.add("Choose a registered business module");
        }

        if (!definition.path("triggerType").asText().equals("MANUAL")) {
            errors.add("Only manual draft triggers are currently supported");
        }

        if (definition.path("steps").isEmpty()) {
            errors.add("Add at least one registered tool step");
        }

        try {
            WorkflowControlPolicy.validateGraph(definition);
        } catch (ResponseStatusException invalid) {
            errors.add(invalid.getReason());
        }

        var keys = new HashSet<String>();

        for (var step : definition.path("steps")) {
            var issues = new ArrayList<String>();

            if (step.path("key").asText().isBlank()
                    || !keys.add(step.path("key").asText())) {
                issues.add("Step key must be unique and nonempty");
            }

            if (step.path("name").asText().isBlank()) {
                issues.add("Step name is required");
            }

            try {
                if (WorkflowControlPolicy.control(step)) {
                    WorkflowReadPolicy.validateStep(registry, step);
                } else {
                    var tool = registry.authorize(
                            AiIntent.valueOf(step.path("tool").asText())
                    );
                    registry.validate(tool, step.path("parameters"));
                }
            } catch (Exception denied) {
                issues.add(
                        "Tool unavailable, permission denied or parameters "
                                + "invalid against the registered schema"
                );
            }

            steps.add(Map.of(
                    "key", step.path("key").asText(),
                    "status", issues.isEmpty() ? "VALIDATED" : "INVALID",
                    "errors", issues
            ));
            errors.addAll(issues);
        }

        return Map.of(
                "valid", errors.isEmpty(),
                "mode", "VALIDATION_ONLY",
                "executed", false,
                "errors", errors,
                "steps", steps,
                "checkedAt", Instant.now()
        );
    }

    @ExceptionHandler(
            org.springframework.orm.ObjectOptimisticLockingFailureException.class
    )
    public org.springframework.http.ResponseEntity<Map<String, String>> conflict() {
        return org.springframework.http.ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "message",
                        "Draft changed. Reload before saving."
                ));
    }

    private WorkflowDraft owned(UUID id, long owner) {
        return drafts.findByIdAndOwnerId(id, owner)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Draft not found"
                ));
    }

    private void apply(WorkflowDraft draft, JsonNode definition) {
        draft.setName(definition.path("name").asText().trim());
        draft.setDefinition(definition.toString());
        draft.setUpdatedAt(Instant.now());
    }

    private void checkDraft(JsonNode definition) {
        if (definition == null
                || !definition.isObject()
                || definition.toString().length() > 65536) {
            throw bad("Draft must be an object smaller than 64 KiB");
        }

        var name = definition.path("name").asText().trim();

        if (name.length() < 3 || name.length() > 120) {
            throw bad("Name must contain 3–120 characters");
        }

        if (definition.path("description").asText().length() > 500) {
            throw bad("Description exceeds 500 characters");
        }

        if (!definition.path("steps").isArray()
                || definition.path("steps").size() > 50) {
            throw bad("Steps must be an array with at most 50 items");
        }

        rejectSensitive(definition);
    }

    private void rejectSensitive(JsonNode node) {
        if (node.isObject()) {
            var fields = node.fields();

            while (fields.hasNext()) {
                var field = fields.next();

                if (field.getKey().matches(
                        "(?i).*(password|secret|token|authorization|"
                                + "credential|cardnumber|cvv).*"
                )) {
                    throw bad(
                            "Store resource references, never credentials "
                                    + "or payment secrets"
                    );
                }

                rejectSensitive(field.getValue());
            }
        } else if (node.isArray()) {
            node.forEach(this::rejectSensitive);
        }
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}