package com.example.eshop.ai.workflow;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.*;

@RestController
@RequestMapping("/api/ai/admin/workflows")
@RequiredArgsConstructor
public class WorkflowExecutionController {

 private final WorkflowPublicationService publications;
 private final WorkflowManualRunner runner;
 private final WorkflowRunLedger ledger;

 public record Publish(@NotNull Long revision) {}

 public record Execute(@NotNull UUID idempotencyKey) {}

 @PostMapping("/{id}/publish")
 public WorkflowPublishedVersion publish(
         @PathVariable UUID id,
         @Valid @RequestBody Publish body
 ) {
  return publications.publish(id, body.revision());
 }

 @GetMapping("/{id}/versions")
 public List<WorkflowPublishedVersion> versions(@PathVariable UUID id) {
  return publications.list(id);
 }

 @PostMapping("/versions/{id}/execute")
 public WorkflowRunLedger.Detail execute(
         @PathVariable UUID id,
         @Valid @RequestBody Execute body,
         @RequestHeader("Authorization") String bearer
 ) {
  return runner.execute(id, body.idempotencyKey(), bearer);
 }

 @GetMapping("/summary")
 public Map<String, Object> summary() {
  return ledger.summary();
 }

 @GetMapping("/executions")
 public Map<String, Object> executions(
         @RequestParam(defaultValue = "0") int page,
         @RequestParam(defaultValue = "") String search,
         @RequestParam(defaultValue = "") String status,
         @RequestParam(required = false) UUID workflowId,
         @RequestParam(required = false)
         @org.springframework.format.annotation.DateTimeFormat(
                 iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
         ) java.time.Instant from,
         @RequestParam(required = false)
         @org.springframework.format.annotation.DateTimeFormat(
                 iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
         ) java.time.Instant until
 ) {
  return ledger.list(page, search, status, workflowId, from, until);
 }

 @GetMapping("/executions/{id}/logs")
 public Map<String, Object> logs(
         @PathVariable UUID id,
         @RequestParam(defaultValue = "0") int page
 ) {
  return ledger.logs(id, page);
 }

 @GetMapping("/executions/{id}")
 public WorkflowRunLedger.Detail detail(@PathVariable UUID id) {
  return ledger.detail(id);
 }

 @PostMapping("/executions/{id}/cancel")
 public WorkflowRunLedger.Detail cancel(@PathVariable UUID id) {
  return ledger.cancel(id);
 }

 @PostMapping("/executions/{id}/rerun")
 public WorkflowRunLedger.Detail rerun(
         @PathVariable UUID id,
         @Valid @RequestBody Execute body,
         @RequestHeader("Authorization") String bearer
 ) {
  var previous = ledger.detail(id);

  if (previous.run().getStatus().equals("RUNNING")) {
   throw new org.springframework.web.server.ResponseStatusException(
           org.springframework.http.HttpStatus.CONFLICT,
           "Wait for completion or cancel the active execution "
                   + "before rerunning"
   );
  }

  return runner.execute(
          previous.run().getPublishedVersionId(),
          body.idempotencyKey(),
          bearer
  );
 }

 @PostMapping("/executions/{id}/reconcile")
 public WorkflowRunLedger.Detail reconcile(@PathVariable UUID id) {
  return ledger.reconcile(id);
 }
}