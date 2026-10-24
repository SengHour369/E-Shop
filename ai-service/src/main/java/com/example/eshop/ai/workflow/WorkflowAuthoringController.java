package com.example.eshop.ai.workflow;

import com.example.eshop.ai.client.AiProviderClient;
import com.example.eshop.ai.registry.AiToolRegistry;
import com.example.eshop.ai.enums.AiToolRisk;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

/** Authoring endpoints never invoke business tools or mutate domain resources. */
@RestController
@RequestMapping("/api/ai/admin/workflows")
@RequiredArgsConstructor
public class WorkflowAuthoringController {

 private final WorkflowPublicationService publications;
 private final WorkflowController validation;
 private final AiToolRegistry registry;
 private final AiProviderClient provider;
 private final ObjectMapper mapper;

 @ExceptionHandler(ResponseStatusException.class)
 public org.springframework.http.ResponseEntity<Map<String, String>> rejected(
         ResponseStatusException failure
 ) {
  return org.springframework.http.ResponseEntity
          .status(failure.getStatusCode())
          .body(Map.of(
                  "message",
                  failure.getReason() == null
                          ? "Workflow proposal failed"
                          : failure.getReason()
          ));
 }

 public record Proposal(
         @NotBlank @Size(min = 8, max = 2000) String prompt
 ) {}

 @GetMapping("/templates")
 public List<Map<String, Object>> templates() {
  publications.actor();
  return registry.available().stream()
          .filter(t ->
                  WorkflowReadPolicy.ALLOWED.contains(t.toolName())
                          && t.risk() == AiToolRisk.READ_ONLY
          )
          .map(t -> {
           var definition = mapper.createObjectNode();
           definition.put(
                   "name",
                   t.description().substring(
                           0,
                           Math.min(120, t.description().length())
                   )
           );
           definition.put("description", t.description());
           definition.put("module", t.service());
           definition.put("triggerType", "MANUAL");
           var step = definition.putArray("steps").addObject();
           step.put("key", UUID.randomUUID().toString());
           step.put("name", t.toolName().name());
           step.put("tool", t.toolName().name());
           var parameters = step.putObject("parameters");
           t.requestSchema().forEach((name, schema) -> {
            if (schema.required()) {
             parameters.putNull(name);
            }
           });
           return Map.<String, Object>of(
                   "id", t.toolName().name(),
                   "name", t.description(),
                   "module", t.service(),
                   "definition", definition,
                   "requiresInput", t.requestSchema()
                           .values().stream()
                           .anyMatch(s -> s.required())
           );
          })
          .toList();
 }

 @PostMapping("/propose")
 public Map<String, Object> propose(@Valid @RequestBody Proposal body) {
  publications.actor();
  var allowed = registry.available().stream()
          .filter(t ->
                  WorkflowReadPolicy.ALLOWED.contains(t.toolName())
                          && t.risk() == AiToolRisk.READ_ONLY
          )
          .toList();

  if (allowed.isEmpty()) {
   throw new ResponseStatusException(
           HttpStatus.FORBIDDEN,
           "No executable workflow tools are permitted"
   );
  }

  com.example.eshop.ai.dto.AiIntentResult result;

  try {
   result = provider.detect(body.prompt(), allowed);
  } catch (com.example.eshop.ai.service.AiFailure failure) {
   throw new ResponseStatusException(
           HttpStatus.SERVICE_UNAVAILABLE,
           "The AI provider could not generate a proposal. "
                   + "Try again later or configure steps manually."
   );
  }

  if (result == null
          || result.intent() == null
          || !Double.isFinite(result.confidence())
          || result.confidence() < 0.7
          || allowed.stream().noneMatch(t ->
          t.toolName() == result.intent())) {
   throw new ResponseStatusException(
           HttpStatus.UNPROCESSABLE_ENTITY,
           "The AI provider could not propose a supported workflow. "
                   + "Describe one read operation and its explicit "
                   + "resource references."
   );
  }

  var tool = registry.authorize(result.intent());
  registry.validate(tool, result.parameters());
  var definition = mapper.createObjectNode();
  definition.put(
          "name",
          "AI proposal: " + result.intent().name()
  );
  definition.put("description", tool.description());
  definition.put("module", tool.service());
  definition.put("triggerType", "MANUAL");
  var step = definition.putArray("steps").addObject();
  step.put("key", UUID.randomUUID().toString());
  step.put("name", result.intent().name());
  step.put("tool", result.intent().name());
  step.set("parameters", result.parameters());
  var checked = validation.validate(new WorkflowController.Write(
          null,
          definition
  ));
  return Map.of(
          "definition", definition,
          "validation", checked,
          "confidence", result.confidence(),
          "executed", false
  );
 }
}