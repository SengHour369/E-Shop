package com.example.eshop.ai.workflow;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.example.eshop.ai.registry.AiToolRegistry;
import java.util.*;
import java.time.Instant;

@RestController @RequestMapping("/api/ai/admin/workflows") @RequiredArgsConstructor
public class WorkflowTriggerController {
 private final WorkflowPublicationService publications;private final WorkflowPublishedRepository versions;
 private final WorkflowTriggerRepository triggers;private final WorkflowTriggerJobRepository jobs;
 private final WorkflowAuthorizationClient authorization;private final WorkflowTriggerStore store;
 private final AiToolRegistry registry;private final WorkflowRunRepository runs;private final WorkflowRunLedger ledger;
 @GetMapping("/{workflowId}/trigger") public WorkflowTrigger current(@PathVariable UUID workflowId){publications.list(workflowId);return triggers.findByWorkflowIdAndOwnerId(workflowId,publications.actor()).orElse(null);}
 @GetMapping("/triggers") public List<WorkflowTrigger> list(){return triggers.findByOwnerIdOrderByActivatedAtDesc(publications.actor());}
 @PostMapping("/versions/{id}/activate-trigger") public WorkflowTrigger activate(@PathVariable UUID id,@RequestHeader("Authorization")String bearer){
  long owner=publications.actor();var version=versions.findByIdAndOwnerId(id,owner).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Published version not found"));
  var definition=publications.parse(version.getDefinition());WorkflowTriggerPolicy.validate(definition);var type=definition.path("triggerType").asText();if(type.equals("MANUAL"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Manual workflows do not need a background trigger");
  for(var step:definition.path("steps"))WorkflowReadPolicy.validateStep(registry,step);
  var config=definition.path("trigger");var now=Instant.now();var expires=now.plusSeconds(config.path("validForDays").asLong()*86400);
  Instant first=type.equals("SCHEDULED")?Instant.parse(config.path("firstRunAt").asText()):null;
  if(first!=null&&!first.isBefore(expires))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"The first run must be before the authorization expires");
  var grant=authorization.create(bearer,new WorkflowAuthorizationClient.Create(id,expires));
  var trigger=new WorkflowTrigger();trigger.setId(UUID.randomUUID());trigger.setWorkflowId(version.getWorkflowId());trigger.setVersionId(id);trigger.setOwnerId(owner);trigger.setTriggerType(type);trigger.setState("ACTIVE");trigger.setGrantId(grant.id());trigger.setAuthorizationExpiresAt(grant.expiresAt());trigger.setActivatedAt(now);
  if(type.equals("SCHEDULED")){trigger.setNextRunAt(first.isBefore(now)?now:first);trigger.setIntervalMinutes(config.path("intervalMinutes").asInt());}
  else{trigger.setEventType(config.path("eventType").asText());trigger.setEventName(config.path("eventName").asText(null));if(config.hasNonNull("orderId"))trigger.setOrderId(config.path("orderId").asLong());}
  var previousGrant=triggers.findByWorkflowIdAndOwnerId(version.getWorkflowId(),owner).map(WorkflowTrigger::getGrantId);
  WorkflowTrigger saved;try{saved=store.activate(trigger);}catch(RuntimeException failure){authorization.revoke(bearer,grant.id());throw failure;}
  previousGrant.ifPresent(old->authorization.revoke(bearer,old));return saved;
 }
 @PostMapping("/triggers/{id}/pause") public WorkflowTrigger pause(@PathVariable UUID id,@RequestHeader("Authorization")String bearer){
  var trigger=store.pause(id,publications.actor());authorization.revoke(bearer,trigger.getGrantId());
  for(var job:jobs.findByTriggerIdAndStatus(id,"RUNNING"))if(runs.findByIdAndOwnerId(job.getId(),trigger.getOwnerId()).filter(r->r.getStatus().equals("RUNNING")).isPresent())ledger.cancel(job.getId());return trigger;
 }
 @GetMapping("/triggers/{id}/jobs") public Map<String,Object> jobs(@PathVariable UUID id,@RequestParam(defaultValue="0")int page){long owner=publications.actor();triggers.findById(id).filter(t->t.getOwnerId()==owner).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Trigger not found"));if(page<0||page>100000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid page");var result=jobs.findByTriggerIdAndOwnerId(id,owner,PageRequest.of(page,20,Sort.by("createdAt").descending()));return Map.of("items",result.getContent(),"total",result.getTotalElements(),"totalPages",result.getTotalPages());}
 public record Signal(String name,UUID idempotencyKey){}
 @PostMapping("/signals") public Map<String,Object> signal(@RequestBody Signal body){return Map.of("eventId",store.signal(publications.actor(),body.name(),body.idempotencyKey()),"status","ACCEPTED");}
}
