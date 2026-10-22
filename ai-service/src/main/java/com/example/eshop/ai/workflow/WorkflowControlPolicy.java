package com.example.eshop.ai.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

/** Fixed forward-only control flow. No expressions, scripts, arbitrary URLs or domain mutations. */
public final class WorkflowControlPolicy {
 private WorkflowControlPolicy(){}
 public static final Set<String> CODES=Set.of("WORKFLOW_CONDITION","WORKFLOW_END","WORKFLOW_DELAY","WORKFLOW_REPEAT");
 public static boolean control(JsonNode step){return CODES.contains(step.path("tool").asText());}
 public static void validate(JsonNode step){
  var parameters=step.path("parameters");if(!parameters.isObject())throw bad("Control parameters must be an object");
  if(step.path("tool").asText().equals("WORKFLOW_END")){if(!parameters.isEmpty())throw bad("End steps do not accept parameters");return;}
  if(step.path("tool").asText().equals("WORKFLOW_DELAY")){if(parameters.size()!=1||!parameters.path("seconds").isIntegralNumber()||!parameters.path("seconds").canConvertToInt()||parameters.path("seconds").asInt()<0||parameters.path("seconds").asInt()>30)throw bad("Manual delay requires seconds between 0 and 30");return;}
  if(step.path("tool").asText().equals("WORKFLOW_REPEAT")){if(parameters.size()!=3||!parameters.path("count").isIntegralNumber()||!parameters.path("count").canConvertToInt()||parameters.path("count").asInt()<1||parameters.path("count").asInt()>10||!parameters.path("tool").isTextual()||!parameters.path("parameters").isObject())throw bad("Repeat requires a tool, parameters object and count between 1 and 10");return;}
  parameters.fieldNames().forEachRemaining(key->{if(!Set.of("sourceStep","path","operator","value","trueNext","falseNext").contains(key))throw bad("Unknown condition parameter: "+key);});
  for(String key:List.of("sourceStep","path","operator","trueNext","falseNext"))if(!parameters.path(key).isTextual())throw bad("Condition "+key+" must be text");
  if(parameters.path("sourceStep").asText().isBlank())throw bad("Choose an earlier source step");
  String path=parameters.path("path").asText();if(path.length()>500||!path.isEmpty()&&!path.startsWith("/"))throw bad("Condition path must be a JSON Pointer, for example /data/quantity");
  try{com.fasterxml.jackson.core.JsonPointer.compile(path);}catch(IllegalArgumentException invalid){throw bad("Invalid JSON Pointer");}
  String operator=parameters.path("operator").asText();if(!Set.of("EQ","NE","GT","GTE","LT","LTE","EXISTS").contains(operator))throw bad("Unsupported condition operator");
  if(!operator.equals("EXISTS")&&(!parameters.has("value")||parameters.path("value").isContainerNode()))throw bad("Condition value must be a scalar");
  if(Set.of("GT","GTE","LT","LTE").contains(operator)&&!parameters.path("value").isNumber())throw bad("Numeric comparisons require a numeric value");
 }
 public static void validateGraph(JsonNode definition){
  var positions=new LinkedHashMap<String,Integer>();int index=0;
  for(var step:definition.path("steps")){String key=step.path("key").asText();if(key.isBlank()||key.equals("$end")||positions.put(key,index++)!=null)throw bad("Step keys must be unique; $end is reserved");}
  index=0;int delaySeconds=0;int calls=0;
  for(var step:definition.path("steps")){if(control(step))validate(step);if(step.path("tool").asText().equals("WORKFLOW_DELAY"))delaySeconds+=step.path("parameters").path("seconds").asInt();if(step.path("tool").asText().equals("WORKFLOW_REPEAT"))calls+=step.path("parameters").path("count").asInt();else if(!control(step))calls++;var parameters=step.path("parameters");if(step.path("tool").asText().equals("WORKFLOW_CONDITION")){
   var source=positions.get(parameters.path("sourceStep").asText());if(source==null||source>=index)throw bad("A condition must reference an earlier step");
   for(String branch:List.of("trueNext","falseNext"))target(parameters.path(branch).asText(),positions,index);
  }if(step.has("next")){if(!step.path("next").isTextual())throw bad("Next step must be a step key");target(step.path("next").asText(),positions,index);}index++;}
  if(delaySeconds>30)throw bad("Total manual delay cannot exceed 30 seconds per run");if(calls>100)throw bad("A workflow cannot exceed 100 read calls per run");
 }
 private static void target(String key,Map<String,Integer> positions,int from){if(!key.equals("$end")&&(!positions.containsKey(key)||positions.get(key)<=from))throw bad("Branch targets must be later steps or $end");}
 public static boolean evaluate(JsonNode step,Map<String,JsonNode> outputs){
  validate(step);var parameters=step.path("parameters");var source=outputs.get(parameters.path("sourceStep").asText());if(source==null)throw bad("Condition source did not execute on this branch");var actual=source.at(parameters.path("path").asText());String operator=parameters.path("operator").asText();var value=parameters.path("value");
  if(operator.equals("EXISTS"))return !actual.isMissingNode()&&!actual.isNull();
  if(actual.isMissingNode())throw bad("Condition path is absent in the real source response");
  if(operator.equals("EQ"))return equal(actual,value);if(operator.equals("NE"))return !equal(actual,value);
  if(!actual.isNumber())throw bad("The source value is not numeric");int comparison=actual.decimalValue().compareTo(value.decimalValue());
  return switch(operator){case "GT"->comparison>0;case "GTE"->comparison>=0;case "LT"->comparison<0;case "LTE"->comparison<=0;default->throw bad("Unsupported condition operator");};
 }
 private static boolean equal(JsonNode a,JsonNode b){return a.isNumber()&&b.isNumber()?a.decimalValue().compareTo(b.decimalValue())==0:a.equals(b);}
 public static int next(JsonNode definition,int position,String key){if(key.equals("$end"))return definition.path("steps").size();for(int i=position+1;i<definition.path("steps").size();i++)if(definition.path("steps").get(i).path("key").asText().equals(key))return i;throw bad("Invalid forward branch target");}
 private static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
