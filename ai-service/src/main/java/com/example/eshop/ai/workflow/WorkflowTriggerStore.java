package com.example.eshop.ai.workflow;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.example.eshop.common.audit.*;
import com.example.eshop.common.automation.CommerceEvent;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service @RequiredArgsConstructor
public class WorkflowTriggerStore {
 private final WorkflowTriggerRepository triggers;private final WorkflowTriggerJobRepository jobs;
 private final WorkflowReceivedEventRepository events;private final WorkflowRunRepository runs;
 private final WorkflowRunStepRepository steps;private final AuditLogService audit;
 @Transactional public WorkflowTrigger activate(WorkflowTrigger value){
  var old=triggers.findByWorkflowIdAndOwnerId(value.getWorkflowId(),value.getOwnerId());
  if(old.isPresent()){var existing=old.get();cancelQueued(existing.getId());value.setId(existing.getId());value.setRevision(existing.getRevision());}
  var saved=triggers.saveAndFlush(value);audit.record(AuditEvent.success(AuditAction.ENABLE,"WORKFLOW_TRIGGER",saved.getId(),null,Map.of("versionId",saved.getVersionId().toString(),"type",saved.getTriggerType())));return saved;
 }
 @Transactional public WorkflowTrigger pause(UUID id,long owner){var trigger=ownedLock(id,owner);trigger.setState("PAUSED");cancelQueued(id);audit.record(AuditEvent.success(AuditAction.DISABLE,"WORKFLOW_TRIGGER",id,null,Map.of("state","PAUSED")));return trigger;}
 private void cancelQueued(UUID id){for(var job:jobs.findByTriggerIdAndStatus(id,"QUEUED")){job.setStatus("CANCELLED");job.setFinishedAt(Instant.now());}}
 private WorkflowTrigger ownedLock(UUID id,long owner){return triggers.locked(id).filter(t->t.getOwnerId()==owner).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Trigger not found"));}
 @Transactional public void enqueueDue(){
  var now=Instant.now();for(var expired:triggers.findByStateAndAuthorizationExpiresAtBefore("ACTIVE",now)){expired.setState("AUTHORIZATION_EXPIRED");cancelQueued(expired.getId());}
  for(var trigger:triggers.due(now,PageRequest.of(0,50))){
   if(!trigger.getAuthorizationExpiresAt().isAfter(now)){trigger.setState("AUTHORIZATION_EXPIRED");continue;}
   var due=trigger.getNextRunAt();enqueue(trigger,UUID.nameUUIDFromBytes((trigger.getId()+":"+due).getBytes(StandardCharsets.UTF_8)),"SCHEDULED",null);
   int interval=trigger.getIntervalMinutes();if(interval==0){trigger.setNextRunAt(null);trigger.setState("COMPLETED");}
   else{long seconds=interval*60L;long skipped=Math.max(0,Duration.between(due,now).getSeconds())/seconds;trigger.setNextRunAt(due.plusSeconds((skipped+1)*seconds));}
  }
 }
 @Transactional public UUID signal(long owner,String name,UUID eventId){
  if(name==null||!name.matches("[A-Za-z0-9][A-Za-z0-9._:-]{2,119}")||eventId==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Provide a custom event name and idempotency key");
  // The stored event identity includes the owner, so one actor cannot suppress another's event.
  UUID key=UUID.nameUUIDFromBytes((owner+":"+eventId).getBytes(StandardCharsets.UTF_8));if(events.existsById(key))return key;
  var received=new WorkflowReceivedEvent();received.setId(key);received.setReceivedAt(Instant.now());received.setEventType("CUSTOM");events.saveAndFlush(received);
  for(var trigger:triggers.findByStateAndTriggerTypeAndEventType("ACTIVE","EVENT","CUSTOM"))if(trigger.getOwnerId()==owner&&name.equals(trigger.getEventName()))enqueue(trigger,key,"CUSTOM",null);
  return key;
 }
 @Transactional public void commerce(CommerceEvent event){
  event.validate();if(events.existsById(event.eventId()))return;var received=new WorkflowReceivedEvent();received.setId(event.eventId());received.setReceivedAt(Instant.now());received.setEventType(event.type().name());events.saveAndFlush(received);
  for(var trigger:triggers.findByStateAndTriggerTypeAndEventType("ACTIVE","EVENT",event.type().name()))if(!event.occurredAt().isBefore(trigger.getActivatedAt())&&(trigger.getOrderId()==null||trigger.getOrderId()==event.orderId()))enqueue(trigger,event.eventId(),event.type().name(),event.requestId());
 }
 private void enqueue(WorkflowTrigger trigger,UUID event,String source,String requestId){
  if(!trigger.getAuthorizationExpiresAt().isAfter(Instant.now())){trigger.setState("AUTHORIZATION_EXPIRED");return;}
  var id=UUID.nameUUIDFromBytes((trigger.getId()+":"+event).getBytes(StandardCharsets.UTF_8));if(jobs.existsById(id))return;
  var job=new WorkflowTriggerJob();job.setId(id);job.setTriggerId(trigger.getId());job.setVersionId(trigger.getVersionId());job.setOwnerId(trigger.getOwnerId());job.setEventKey(event);job.setSource(source);job.setStatus("QUEUED");job.setCreatedAt(Instant.now());job.setRequestId(requestId);jobs.save(job);
 }
 @Transactional public WorkflowTriggerJob claim(){
  var pending=jobs.queued(PageRequest.of(0,1));if(pending.isEmpty())return null;var job=pending.get(0);var trigger=triggers.findById(job.getTriggerId()).orElse(null);
  if(trigger==null||!trigger.getVersionId().equals(job.getVersionId())||!Set.of("ACTIVE","COMPLETED").contains(trigger.getState())){job.setStatus("CANCELLED");job.setFinishedAt(Instant.now());return null;}
  job.setStatus("RUNNING");job.setStartedAt(Instant.now());return job;
 }
 @Transactional public void finish(UUID id,String status,String error,boolean authorizationFailure){
  var job=jobs.findById(id).orElseThrow();job.setStatus(status);job.setErrorCode(error);job.setFinishedAt(Instant.now());if(runs.existsById(id))job.setExecutionId(id);
  var trigger=triggers.locked(job.getTriggerId()).orElseThrow();if(!trigger.getVersionId().equals(job.getVersionId()))return;trigger.setLastRunAt(job.getStartedAt());trigger.setLastStatus(status);
  if(authorizationFailure)trigger.setState("AUTHORIZATION_REQUIRED");
 }
 @Transactional public void recover(){
  for(var job:jobs.findByStatusAndStartedAtBefore("RUNNING",Instant.now().minusSeconds(900))){
   job.setStatus("UNKNOWN");job.setFinishedAt(Instant.now());job.setErrorCode("WORKER_INTERRUPTED");
   runs.findById(job.getId()).filter(r->r.getOwnerId()==job.getOwnerId()&&r.getStatus().equals("RUNNING")).ifPresent(run->{run.setStatus("UNKNOWN");run.setFinishedAt(Instant.now());job.setExecutionId(run.getId());for(var step:steps.findByRunIdOrderByPositionAsc(run.getId()))if(Set.of("RUNNING","PENDING").contains(step.getStatus())){step.setStatus(step.getStatus().equals("RUNNING")?"UNKNOWN":"SKIPPED");step.setErrorCode("WORKER_INTERRUPTED");step.setFinishedAt(Instant.now());}});
  }
 }
}
