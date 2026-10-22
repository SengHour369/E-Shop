package com.example.eshop.ai.workflow;

import com.example.eshop.ai.enums.*;
import com.example.eshop.ai.registry.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;

/** Explicit safe subset: some legacy tools labeled READ_ONLY still create domain review/QR records. */
public final class WorkflowReadPolicy {
 private WorkflowReadPolicy() {}
 public static final Set<AiIntent> ALLOWED=Set.of(AiIntent.PRODUCT_GET,AiIntent.PRODUCT_DETAIL,AiIntent.PRODUCT_VARIANTS,
  AiIntent.SKU_GET,AiIntent.PRODUCT_STOCK_CHECK,AiIntent.PRODUCT_SEARCH,AiIntent.INVENTORY_GET,AiIntent.INVENTORY_LOW_STOCK,AiIntent.PROMOTION_GET,AiIntent.ORDER_GET,AiIntent.MY_ORDERS,AiIntent.MY_ORDER_STATUS,AiIntent.MY_PAYMENT_STATUS,
  AiIntent.MY_RETURNS,AiIntent.MY_NOTIFICATIONS,AiIntent.ADMIN_ORDER_LIST,AiIntent.ADMIN_PAYMENT_LIST,AiIntent.ADMIN_RETURN_LIST,
  AiIntent.ADMIN_ORDER_SUMMARY,AiIntent.ADMIN_USER_LIST,AiIntent.ADMIN_REVENUE_SUMMARY,AiIntent.ADMIN_AUDIT_LOG);
 public static void validateStep(AiToolRegistry registry,JsonNode step){if(WorkflowControlPolicy.control(step)){WorkflowControlPolicy.validate(step);if(step.path("tool").asText().equals("WORKFLOW_REPEAT"))authorize(registry,step.path("parameters"));}else authorize(registry,step);}
 public static AiToolDefinition authorize(AiToolRegistry registry,JsonNode step) {
  AiIntent intent;
  try {intent=AiIntent.valueOf(step.path("tool").asText());}
  catch(IllegalArgumentException ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown workflow tool");}
  if(!ALLOWED.contains(intent)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"This workflow engine currently permits registered read tools only; writes and approvals are unavailable");
  var tool=registry.authorize(intent);
  if(tool.risk()!=AiToolRisk.READ_ONLY) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tool is not read-only");
  registry.validate(tool,step.path("parameters"));return tool;
 }
}
