package com.example.eshop.ai.workflow;
import com.fasterxml.jackson.databind.JsonNode;
import com.example.eshop.common.automation.CommerceEvent;
import java.time.Instant;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
public final class WorkflowTriggerPolicy {
 private WorkflowTriggerPolicy(){}
 public static final Set<String> TYPES=Set.of("MANUAL","SCHEDULED","EVENT");
 public static void validate(JsonNode definition){
  var type=definition.path("triggerType").asText();if(!TYPES.contains(type))throw bad("Choose Manual, Scheduled or Event Based");
  if(type.equals("MANUAL"))return;var trigger=definition.path("trigger");if(!trigger.isObject())throw bad("Configure the workflow trigger");
  if(!trigger.path("validForDays").isIntegralNumber()||trigger.path("validForDays").asInt()<1||trigger.path("validForDays").asInt()>30)throw bad("Choose an authorization duration from 1 to 30 days");
  if(type.equals("SCHEDULED")){
   try{Instant.parse(trigger.path("firstRunAt").asText());}catch(Exception e){throw bad("Choose the first scheduled run time");}
   if(!trigger.path("intervalMinutes").isIntegralNumber()||trigger.path("intervalMinutes").asInt()<0||trigger.path("intervalMinutes").asInt()>525600)throw bad("Repeat interval must be 0 (once) or 1–525600 minutes");
  }else{
   var event=trigger.path("eventType").asText();
   if(!event.equals("CUSTOM")){try{CommerceEvent.Type.valueOf(event);}catch(Exception e){throw bad("Choose a supported event type");}}
   else if(!trigger.path("eventName").asText().matches("[A-Za-z0-9][A-Za-z0-9._:-]{2,119}"))throw bad("Custom event name must contain 3–120 letters, numbers, dots, colons or hyphens");
   if(trigger.hasNonNull("orderId")&&(!trigger.path("orderId").isIntegralNumber()||trigger.path("orderId").asLong()<1))throw bad("Order filter must be a positive order ID");
  }
 }
 private static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
