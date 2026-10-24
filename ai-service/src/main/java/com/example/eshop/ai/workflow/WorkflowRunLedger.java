package com.example.eshop.ai.workflow;

import com.example.eshop.ai.registry.AiToolRegistry;
import com.example.eshop.common.audit.*;
import com.example.eshop.common.request.RequestIds;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.*;

/** Every transition commits before/after the remote call, never holding a DB transaction across business-tool HTTP. */
@Service
@RequiredArgsConstructor
public class WorkflowRunLedger {

 private final WorkflowRunRepository runs;
 private final WorkflowRunStepRepository steps;
 private final WorkflowPublishedRepository versions;
 private final WorkflowPublicationService publications;
 private final AiToolRegistry registry;
 private final AuditLogService audit;
 private final AuditLogRepository auditLogs;
 private final WorkflowDraftRepository drafts;

 public record Claim(WorkflowRun run, boolean created) {}

 public record Detail(WorkflowRun run, List<WorkflowRunStep> steps) {}

 @Transactional
 public Claim begin(UUID versionId, UUID key) {
  long owner = publications.actor();
  var existing = runs.findById(key);

  if (existing.isPresent()) {
   return existing(existing.get(), versionId, owner);
  }

  var version = versions.findByIdAndOwnerId(versionId, owner)
          .orElseThrow(() -> new ResponseStatusException(
                  HttpStatus.NOT_FOUND,
                  "Published version not found"
          ));
  var definition = publications.parse(version.getDefinition());
  WorkflowControlPolicy.validateGraph(definition);

  for (var step : definition.path("steps")) {
   WorkflowReadPolicy.validateStep(registry, step);
  }

  var run = new WorkflowRun();
  run.setId(key);
  run.setPublishedVersionId(versionId);
  run.setOwnerId(owner);
  run.setStatus("RUNNING");
  run.setStartedAt(Instant.now());
  run.setRequestId(RequestIds.resolve(RequestIds.current()));
  run.setTraceId(MDC.get("traceId"));
  runs.saveAndFlush(run);
  int position = 0;

  for (var item : definition.path("steps")) {
   var step = new WorkflowRunStep();
   step.setId(UUID.randomUUID());
   step.setRunId(key);
   step.setPosition(position++);
   step.setStepKey(item.path("key").asText());
   step.setName(item.path("name").asText());
   step.setTool(item.path("tool").asText());
   step.setStatus("PENDING");
   steps.save(step);
  }

  record(run, "RUNNING", null, null);
  return new Claim(run, true);
 }

 @Transactional(readOnly = true)
 public Claim duplicate(UUID versionId, UUID key) {
  return existing(
          runs.findById(key).orElseThrow(() ->
                  new ResponseStatusException(
                          HttpStatus.CONFLICT,
                          "Execution key unavailable"
                  )),
          versionId,
          publications.actor()
  );
 }

 private Claim existing(WorkflowRun run, UUID version, long owner) {
  if (run.getOwnerId() != owner
          || !run.getPublishedVersionId().equals(version)) {
   throw new ResponseStatusException(
           HttpStatus.CONFLICT,
           "Execution key already used by a different request"
   );
  }

  return new Claim(run, false);
 }

 @Transactional(readOnly = true)
 public Map<String, Object> summary() {
  long owner = publications.actor();
  var after = Instant.now().minus(Duration.ofDays(7));
  return Map.of(
          "totalWorkflows", drafts.countByOwnerId(owner),
          "executions7Days", runs.countByOwnerIdAndStartedAtAfter(
                  owner,
                  after
          ),
          "succeeded7Days", runs.countByOwnerIdAndStatusAndStartedAtAfter(
                  owner,
                  "SUCCEEDED",
                  after
          ),
          "failed7Days", runs.countByOwnerIdAndStatusAndStartedAtAfter(
                  owner,
                  "FAILED",
                  after
          ),
          "running7Days", runs.countByOwnerIdAndStatusAndStartedAtAfter(
                  owner,
                  "RUNNING",
                  after
          ),
          "publishedWorkflows", versions.countPublishedWorkflows(owner)
  );
 }

 @Transactional(readOnly = true)
 public Detail detail(UUID id) {
  var run = owned(id);
  return new Detail(run, steps.findByRunIdOrderByPositionAsc(id));
 }

 @Transactional(readOnly = true)
 public Map<String, Object> list(int page) {
  return list(page, "", "", null, null, null);
 }

 @Transactional(readOnly = true)
 public Map<String, Object> list(
         int page,
         String search,
         String status,
         UUID workflowId,
         Instant from,
         Instant until
 ) {
  if (page < 0
          || page > 100000
          || search.length() > 120
          || !Set.of(
          "",
          "RUNNING",
          "SUCCEEDED",
          "FAILED",
          "UNKNOWN",
          "CANCELLED"
  ).contains(status)
          || from != null && until != null && from.isAfter(until)) {
   throw new ResponseStatusException(
           HttpStatus.BAD_REQUEST,
           "Invalid execution filters"
   );
  }

  long owner = publications.actor();
  org.springframework.data.jpa.domain.Specification<WorkflowRun> scope =
          (root, query, builder) ->
                  builder.equal(root.get("ownerId"), owner);

  if (!status.isBlank()) {
   scope = scope.and((root, query, builder) ->
           builder.equal(root.get("status"), status));
  }

  if (from != null) {
   scope = scope.and((root, query, builder) ->
           builder.greaterThanOrEqualTo(
                   root.get("startedAt"),
                   from
           ));
  }

  if (until != null) {
   scope = scope.and((root, query, builder) ->
           builder.lessThanOrEqualTo(
                   root.get("startedAt"),
                   until
           ));
  }

  if (workflowId != null || !search.isBlank()) {
   scope = scope.and((root, query, builder) -> {
    var sub = query.subquery(UUID.class);
    var version = sub.from(WorkflowPublishedVersion.class);
    var terms = new ArrayList<
            jakarta.persistence.criteria.Predicate
            >();
    terms.add(builder.equal(version.get("ownerId"), owner));

    if (workflowId != null) {
     terms.add(builder.equal(
             version.get("workflowId"),
             workflowId
     ));
    }

    if (!search.isBlank()) {
     String escaped = search.trim()
             .toLowerCase(Locale.ROOT)
             .replace("!", "!!")
             .replace("%", "!%")
             .replace("_", "!_");
     terms.add(builder.like(
             builder.lower(version.get("name")),
             "%" + escaped + "%",
             '!'
     ));
    }

    sub.select(version.get("id"))
            .where(terms.toArray(
                    jakarta.persistence.criteria.Predicate[]::new
            ));
    return root.get("publishedVersionId").in(sub);
   });
  }

  var result = runs.findAll(
          scope,
          PageRequest.of(
                  page,
                  20,
                  Sort.by("startedAt").descending().and(Sort.by("id"))
          )
  );
  var ids = result.getContent().stream()
          .map(WorkflowRun::getPublishedVersionId)
          .distinct()
          .toList();
  var metadata = new LinkedHashMap<String, Object>();

  for (var version : versions.findAllById(ids)) {
   if (version.getOwnerId() == owner) {
    metadata.put(
            version.getId().toString(),
            Map.of(
                    "workflowId", version.getWorkflowId(),
                    "name", version.getName(),
                    "revision", version.getDraftRevision()
            )
    );
   }
  }

  return Map.of(
          "items", result.getContent(),
          "total", result.getTotalElements(),
          "totalPages", result.getTotalPages(),
          "workflows", metadata
  );
 }

 public record Log(
         Long id,
         Instant occurredAt,
         String status,
         String stepKey,
         String tool,
         String errorCode,
         String requestId
 ) {}

 @Transactional(readOnly = true)
 public Map<String, Object> logs(UUID id, int page) {
  owned(id);

  if (page < 0 || page > 100000) {
   throw new ResponseStatusException(
           HttpStatus.BAD_REQUEST,
           "Invalid log page"
   );
  }

  org.springframework.data.jpa.domain.Specification<AuditLog> scope =
          (root, query, builder) -> builder.and(
                  builder.equal(
                          root.get("resourceType"),
                          "AI_WORKFLOW_RUN"
                  ),
                  builder.equal(
                          root.get("resourceId"),
                          id.toString()
                  )
          );
  var records = auditLogs.findAll(
          scope,
          PageRequest.of(
                  page,
                  50,
                  Sort.by("occurredAt").ascending().and(Sort.by("id"))
          )
  );
  var items = records.getContent().stream().map(entry -> {
   var value = entry.getNewValue() == null
           ? com.fasterxml.jackson.databind.node.NullNode.instance
           : publications.parse(entry.getNewValue());
   return new Log(
           entry.getId(),
           entry.getOccurredAt(),
           value.path("status").asText(entry.getResult().name()),
           value.path("stepKey").asText(""),
           value.path("tool").asText(""),
           entry.getErrorCode(),
           entry.getRequestId()
   );
  }).toList();
  return Map.of(
          "items", items,
          "total", records.getTotalElements(),
          "totalPages", records.getTotalPages()
  );
 }

 @Transactional(readOnly = true)
 public JsonNode definition(UUID versionId) {
  return publications.parse(
          versions.findByIdAndOwnerId(
                          versionId,
                          publications.actor()
                  )
                  .orElseThrow(() -> new ResponseStatusException(
                          HttpStatus.NOT_FOUND,
                          "Version not found"
                  ))
                  .getDefinition()
  );
 }

 @Transactional
 public WorkflowRunStep startStep(UUID runId, int position) {
  var run = ownedForUpdate(runId);

  if (!run.getStatus().equals("RUNNING")) {
   throw new ResponseStatusException(
           HttpStatus.CONFLICT,
           "Run is no longer running"
   );
  }

  var step = steps.findByRunIdOrderByPositionAsc(runId).get(position);

  if (!step.getStatus().equals("PENDING")) {
   throw new ResponseStatusException(
           HttpStatus.CONFLICT,
           "Step already claimed"
   );
  }

  step.setStatus("RUNNING");
  step.setStartedAt(Instant.now());
  steps.saveAndFlush(step);
  record(run, "RUNNING", step, null);
  return step;
 }

 @Transactional
 public void finishStep(
         UUID runId,
         int position,
         String status,
         String errorCode,
         String hash,
         Integer size
 ) {
  var run = ownedForUpdate(runId);
  var step = steps.findByRunIdOrderByPositionAsc(runId).get(position);

  if (!run.getStatus().equals("RUNNING")
          || !step.getStatus().equals("RUNNING")) {
   return;
  }

  step.setStatus(status);
  step.setErrorCode(errorCode);
  step.setOutputSha256(hash);
  step.setOutputBytes(size);
  step.setFinishedAt(Instant.now());
  steps.saveAndFlush(step);
  record(run, status, step, errorCode);

  if (!status.equals("SUCCEEDED")) {
   finish(run, status, errorCode);
  }
 }

 @Transactional
 public void iteration(UUID id, int position, int iteration) {
  var run = ownedForUpdate(id);

  if (!run.getStatus().equals("RUNNING")) {
   return;
  }

  var step = steps.findByRunIdOrderByPositionAsc(id).get(position);
  record(run, "READ_ITERATION_" + iteration, step, null);
 }

 @Transactional
 public void skipBranch(UUID id, int from, int until) {
  var run = ownedForUpdate(id);

  if (!run.getStatus().equals("RUNNING")) {
   return;
  }

  for (var step : steps.findByRunIdOrderByPositionAsc(id)) {
   if (step.getPosition() >= from
           && step.getPosition() < until
           && step.getStatus().equals("PENDING")) {
    step.setStatus("SKIPPED");
    step.setErrorCode("BRANCH_NOT_SELECTED");
    step.setFinishedAt(Instant.now());
    record(run, "SKIPPED", step, "BRANCH_NOT_SELECTED");
   }
  }
 }

 @Transactional
 public void succeed(UUID id) {
  var run = ownedForUpdate(id);

  if (run.getStatus().equals("RUNNING")
          && steps.findByRunIdOrderByPositionAsc(id).stream()
          .allMatch(s ->
                  s.getStatus().equals("SUCCEEDED")
                          || s.getStatus().equals("SKIPPED")
                          && "BRANCH_NOT_SELECTED".equals(
                          s.getErrorCode()
                  ))) {
   finish(run, "SUCCEEDED", null);
  }
 }

 // Explicit reconciliation only: abandoned work is UNKNOWN and is never silently replayed.
 @Transactional
 public Detail reconcile(UUID id) {
  var run = ownedForUpdate(id);

  if (run.getStatus().equals("RUNNING")) {
   if (run.getStartedAt().isAfter(
           Instant.now().minus(Duration.ofMinutes(15))
   )) {
    throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Run is not stale; wait at least 15 minutes "
                    + "before reconciliation"
    );
   }

   for (var step : steps.findByRunIdOrderByPositionAsc(id)) {
    if (step.getStatus().equals("RUNNING")) {
     step.setStatus("UNKNOWN");
     step.setErrorCode("WORKER_INTERRUPTED");
     step.setFinishedAt(Instant.now());
     record(run, "UNKNOWN", step, "WORKER_INTERRUPTED");
    }
   }

   finish(run, "UNKNOWN", "WORKER_INTERRUPTED");
  }

  return new Detail(run, steps.findByRunIdOrderByPositionAsc(id));
 }

 private void finish(WorkflowRun run, String status, String code) {
  run.setStatus(status);
  run.setFinishedAt(Instant.now());

  for (var step : steps.findByRunIdOrderByPositionAsc(run.getId())) {
   if (step.getStatus().equals("PENDING")) {
    step.setStatus("SKIPPED");
    step.setErrorCode("PREVIOUS_STEP_INCOMPLETE");
    step.setFinishedAt(Instant.now());
   }
  }

  runs.saveAndFlush(run);
  record(run, status, null, code);
 }

 @Transactional
 public Detail cancel(UUID id) {
  var run = ownedForUpdate(id);

  if (!run.getStatus().equals("RUNNING")) {
   throw new ResponseStatusException(
           HttpStatus.CONFLICT,
           "Only running executions can be cancelled"
   );
  }

  for (var step : steps.findByRunIdOrderByPositionAsc(id)) {
   if (step.getStatus().equals("RUNNING")) {
    step.setStatus("UNKNOWN");
    step.setErrorCode("CANCELLED_DURING_REQUEST");
    step.setFinishedAt(Instant.now());
    record(run, "UNKNOWN", step, "CANCELLED_DURING_REQUEST");
   }
  }

  finish(run, "CANCELLED", "USER_CANCELLED");
  return new Detail(run, steps.findByRunIdOrderByPositionAsc(id));
 }

 private WorkflowRun ownedForUpdate(UUID id) {
  return runs.lockOwned(id, publications.actor())
          .orElseThrow(() -> new ResponseStatusException(
                  HttpStatus.NOT_FOUND,
                  "Execution not found"
          ));
 }

 private WorkflowRun owned(UUID id) {
  return runs.findByIdAndOwnerId(id, publications.actor())
          .orElseThrow(() -> new ResponseStatusException(
                  HttpStatus.NOT_FOUND,
                  "Execution not found"
          ));
 }

 private void record(
         WorkflowRun run,
         String status,
         WorkflowRunStep step,
         String code
 ) {
  Map<String, Object> values = new LinkedHashMap<>();
  values.put("runId", run.getId().toString());
  values.put("versionId", run.getPublishedVersionId().toString());
  values.put("status", status);
  values.put("requestId", run.getRequestId());

  if (step != null) {
   values.put("stepId", step.getId().toString());
   values.put("stepKey", step.getStepKey());
   values.put("tool", step.getTool());
  }

  audit.record(
          new AuditEvent(
                  AuditAction.AI_TOOL_EXECUTION,
                  "AI_WORKFLOW_RUN",
                  run.getId().toString(),
                  null,
                  values,
                  status.equals("FAILED") || status.equals("UNKNOWN")
                          ? AuditResult.FAILURE
                          : AuditResult.SUCCESS,
                  code
          )
  );
 }
}