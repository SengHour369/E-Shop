package com.example.eshop.ai.controller;
import com.example.eshop.ai.service.AiQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.Map;
@RestController @RequiredArgsConstructor
public class AiQuotaController {
 private final AiQuotaService quota;
 public record Feedback(@jakarta.validation.constraints.NotNull java.util.UUID requestKey,@jakarta.validation.constraints.Pattern(regexp="up|down") @jakarta.validation.constraints.NotNull String rating) {}
 @PostMapping("/api/ai/feedback") public Map<String,String> feedback(@jakarta.validation.Valid @RequestBody Feedback request){quota.feedback(request.requestKey(),request.rating());return Map.of("rating",request.rating());}
 @GetMapping("/api/ai/quota") public AiQuotaService.Quota quota(){return quota.current();}
 @RestControllerAdvice
 public static class Errors {
  @ExceptionHandler(AiQuotaService.Exhausted.class) public ResponseEntity<?> exhausted(AiQuotaService.Exhausted error){var q=error.quota;return ResponseEntity.status(429).header("Retry-After",Long.toString(Math.max(1,java.time.Duration.between(java.time.Instant.now(),q.resetAt().toInstant()).getSeconds()))).body(Map.of("code","AI_DAILY_LIMIT_EXCEEDED","message",error.getMessage(),"dailyLimit",q.dailyLimit(),"used",q.used(),"remaining",q.remaining(),"resetAt",q.resetAt()));}
 }
}

