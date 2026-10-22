package com.example.eshop.ai.workflow;
import com.example.eshop.ai.registry.AiToolRegistry;
import com.example.eshop.common.audit.*;
import com.example.eshop.common.security.LivePermissionService;
import com.fasterxml.jackson.databind.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@Service @RequiredArgsConstructor
public class WorkflowPublicationService {
 private final WorkflowDraftRepository drafts;
 private final WorkflowPublishedRepository versions;
 private final WorkflowController validation;
 private final AiToolRegistry registry;
 private final LivePermissionService permissions;
 private final ObjectMapper mapper;
 private final AuditLogService audit;
 public long actor(){var actor=permissions.current();if(!actor.administrator())throw new AccessDeniedException("Administrator access required");return actor.userId();}
 @Transactional public WorkflowPublishedVersion publish(UUID workflowId,Long revision) {
  long owner=actor();var draft=drafts.lockOwned(workflowId,owner).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Draft not found"));
  if(!Objects.equals(draft.getRevision(),revision))throw new ResponseStatusException(HttpStatus.CONFLICT,"Draft changed. Reload before publishing.");
  var definition=parse(draft.getDefinition());WorkflowControlPolicy.validateGraph(definition);
  if(!Boolean.TRUE.equals(validation.validate(new WorkflowController.Write(revision,definition)).get("valid")))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Workflow validation failed");
  for(var step:definition.path("steps")) {
   if(step.path("key").asText().length()>120 || step.path("name").asText().length()>120)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Step keys and names must not exceed 120 characters");
   WorkflowReadPolicy.validateStep(registry,step);
  }
  var existing=versions.findByWorkflowIdAndDraftRevision(workflowId,revision);if(existing.isPresent())return existing.get();
  var version=new WorkflowPublishedVersion();version.setId(UUID.randomUUID());version.setWorkflowId(workflowId);version.setDraftRevision(revision);version.setOwnerId(owner);version.setName(draft.getName());version.setDefinition(draft.getDefinition());version.setPublishedAt(Instant.now());versions.saveAndFlush(version);
  audit.record(AuditEvent.success(AuditAction.CREATE,"AI_WORKFLOW_VERSION",version.getId(),null,Map.of("workflowId",workflowId.toString(),"draftRevision",revision,"mode","MANUAL_READ_ONLY")));
  return version;
 }
 @Transactional(readOnly=true) public List<WorkflowPublishedVersion> list(UUID workflowId){long owner=actor();if(drafts.findByIdAndOwnerId(workflowId,owner).isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Workflow not found");return versions.findByWorkflowIdAndOwnerIdOrderByDraftRevisionDesc(workflowId,owner);}
 public JsonNode parse(String json){try{return mapper.readTree(json);}catch(Exception e){throw new IllegalStateException("Stored workflow definition is invalid");}}
}
