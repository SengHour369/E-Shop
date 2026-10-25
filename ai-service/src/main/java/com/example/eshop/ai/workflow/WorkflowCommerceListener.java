package com.example.eshop.ai.workflow;
import com.example.eshop.common.automation.CommerceEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Component @RequiredArgsConstructor
@ConditionalOnProperty(name="workflow.automation.enabled",havingValue="true",matchIfMissing=true)
public class WorkflowCommerceListener {
 private final ObjectMapper mapper;private final WorkflowTriggerStore store;
 @KafkaListener(topics={CommerceEvent.ORDERS,CommerceEvent.PAYMENTS},groupId="ai-workflow-triggers-v1",properties={"auto.offset.reset=latest"})
 public void receive(String body)throws Exception{
  if(body==null||body.length()>16384)throw new IllegalArgumentException("Invalid commerce event size");
  var event=mapper.readValue(body,CommerceEvent.class);event.validate();store.commerce(event);
 }
}
