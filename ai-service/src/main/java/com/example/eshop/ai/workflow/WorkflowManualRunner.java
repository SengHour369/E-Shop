package com.example.eshop.ai.workflow;
import com.example.eshop.ai.registry.AiToolRegistry;
import com.example.eshop.ai.service.AiToolExecutor;
import com.example.eshop.ai.service.AiFailure;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Real authenticated handler calls; no simulation or synthetic success fallback. */
@Service @RequiredArgsConstructor
public class WorkflowManualRunner {
 private final WorkflowRunLedger ledger;
 private final AiToolRegistry registry;
 private final AiToolExecutor executor;
 public WorkflowRunLedger.Detail execute(UUID version,UUID key,String bearer){
  WorkflowRunLedger.Claim claim;
  try {claim=ledger.begin(version,key);}catch(DataIntegrityViolationException race){claim=ledger.duplicate(version,key);}
  if(!claim.created())return ledger.detail(key);
  var definition=ledger.definition(version);WorkflowControlPolicy.validateGraph(definition);int position=0;long outputBytes=0;
  var outputs=new LinkedHashMap<String,com.fasterxml.jackson.databind.JsonNode>();
  while(position<definition.path("steps").size()){
   var item=definition.path("steps").get(position);
   if(!ledger.detail(key).run().getStatus().equals("RUNNING"))return ledger.detail(key);
   try{ledger.startStep(key,position);}catch(org.springframework.web.server.ResponseStatusException conflict){if(!ledger.detail(key).run().getStatus().equals("RUNNING"))return ledger.detail(key);throw conflict;}
   int next=position+1;
   try {
    com.fasterxml.jackson.databind.JsonNode output;
    String code=item.path("tool").asText();
    if(code.equals("WORKFLOW_CONDITION")){
     boolean selected=WorkflowControlPolicy.evaluate(item,outputs);
     output=com.fasterxml.jackson.databind.node.BooleanNode.valueOf(selected);
     next=WorkflowControlPolicy.next(definition,position,item.path("parameters").path(selected?"trueNext":"falseNext").asText());
    }else if(code.equals("WORKFLOW_DELAY")){
     WorkflowControlPolicy.validate(item);long waitStarted=System.nanoTime();long deadline=waitStarted+item.path("parameters").path("seconds").asLong()*1_000_000_000L;
     while(System.nanoTime()<deadline){if(!ledger.detail(key).run().getStatus().equals("RUNNING"))return ledger.detail(key);long remaining=deadline-System.nanoTime();if(remaining>0)Thread.sleep(Math.min(200,Math.max(1,remaining/1_000_000)));}
     output=com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode().put("elapsedMillis",(System.nanoTime()-waitStarted)/1_000_000L);
     if(item.has("next"))next=WorkflowControlPolicy.next(definition,position,item.path("next").asText());
    }else if(code.equals("WORKFLOW_REPEAT")){
     WorkflowReadPolicy.validateStep(registry,item);var parameters=item.path("parameters");var values=com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();long repeatedBytes=0;
     for(int iteration=1;iteration<=parameters.path("count").asInt();iteration++){if(!ledger.detail(key).run().getStatus().equals("RUNNING"))return ledger.detail(key);var tool=WorkflowReadPolicy.authorize(registry,parameters);var value=executor.execute(tool,parameters.path("parameters"),bearer);if(value==null||value.isNull())throw new IllegalStateException("Empty authoritative response");repeatedBytes+=value.toString().getBytes(StandardCharsets.UTF_8).length;if(repeatedBytes+outputBytes>16L*1024*1024)throw new IllegalStateException("Workflow output memory limit exceeded");values.add(value);ledger.iteration(key,position,iteration);}
     output=values;if(item.has("next"))next=WorkflowControlPolicy.next(definition,position,item.path("next").asText());
    }else if(code.equals("WORKFLOW_END")){
     output=com.fasterxml.jackson.databind.node.TextNode.valueOf("END");next=definition.path("steps").size();
    }else{
     var tool=WorkflowReadPolicy.authorize(registry,item);
     output=executor.execute(tool,item.path("parameters"),bearer);
     if(output==null || output.isNull())throw new IllegalStateException("Empty authoritative response");
     if(item.has("next"))next=WorkflowControlPolicy.next(definition,position,item.path("next").asText());
    }
    byte[] bytes=output.toString().getBytes(StandardCharsets.UTF_8);
    outputBytes+=bytes.length;if(outputBytes>16L*1024*1024)throw new IllegalStateException("Workflow output memory limit exceeded");
    outputs.put(item.path("key").asText(),output);
    String digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    ledger.finishStep(key,position,"SUCCEEDED",null,digest,bytes.length);
    if(next>position+1)ledger.skipBranch(key,position+1,next);
   }catch(Exception failure){
    boolean interrupted=failure instanceof InterruptedException;
    boolean denied=failure instanceof AiFailure || failure instanceof org.springframework.security.access.AccessDeniedException || failure instanceof org.springframework.web.server.ResponseStatusException;
    String status=denied?"FAILED":"UNKNOWN";
    try{ledger.finishStep(key,position,status,denied?"TOOL_VALIDATION_OR_PERMISSION_DENIED":"UPSTREAM_RESULT_UNAVAILABLE",null,null);return ledger.detail(key);}finally{if(interrupted)Thread.currentThread().interrupt();}
   }
   position=next;
  }
  ledger.succeed(key);return ledger.detail(key);
 }
}
