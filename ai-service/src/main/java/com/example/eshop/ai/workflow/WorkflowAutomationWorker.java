package com.example.eshop.ai.workflow;

import com.example.eshop.common.automation.ServiceIdentity;
import com.example.eshop.common.jwt.JwtProperties;
import com.example.eshop.common.security.BackgroundIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.MDC;
import java.util.UUID;

@Component @RequiredArgsConstructor
@ConditionalOnProperty(name="workflow.automation.enabled",havingValue="true",matchIfMissing=true)
public class WorkflowAutomationWorker {
 private final WorkflowTriggerStore store;private final WorkflowTriggerRepository triggers;
 private final WorkflowAuthorizationClient authorization;private final JwtProperties jwt;private final WorkflowManualRunner runner;
 @Scheduled(fixedDelayString="${workflow.automation.dispatch-delay-ms:1000}") public void dispatch(){store.enqueueDue();}
 @Scheduled(fixedDelayString="${workflow.automation.worker-delay-ms:500}") public void work(){
  var job=store.claim();if(job==null)return;
  var previous=MDC.get("requestId");MDC.put("requestId",job.getRequestId()==null?"workflow-trigger-"+job.getId():job.getRequestId());
  try{
   var trigger=triggers.findById(job.getTriggerId()).orElseThrow();
   var credential=authorization.exchange(ServiceIdentity.bearer(jwt,"ai-service","SERVICE_AI_WORKFLOW"),new WorkflowAuthorizationClient.Exchange(trigger.getGrantId(),job.getVersionId()));
   if(credential.ownerId()!=job.getOwnerId())throw new IllegalStateException("Workflow owner mismatch");
   var result=BackgroundIdentity.run(credential.ownerId(),credential.username(),"Bearer "+credential.accessToken(),()->runner.execute(job.getVersionId(),job.getId(),"Bearer "+credential.accessToken()));
   var status=result.run().getStatus();store.finish(job.getId(),status.equals("RUNNING")?"UNKNOWN":status,status.equals("RUNNING")?"EXECUTION_ALREADY_RUNNING":null,false);
  }catch(Exception failure){
   boolean denied=failure instanceof org.springframework.security.access.AccessDeniedException||failure instanceof com.example.eshop.ai.service.AiFailure||failure instanceof feign.FeignException e&&(e.status()==401||e.status()==403);
   store.finish(job.getId(),"FAILED",denied?"AUTHORIZATION_REQUIRED":"TRIGGER_EXECUTION_UNAVAILABLE",denied);
  }finally{if(previous==null)MDC.remove("requestId");else MDC.put("requestId",previous);}
 }
 @Scheduled(fixedDelayString="${workflow.automation.recovery-delay-ms:60000}") public void recover(){store.recover();}
}
