package com.example.eshop.ai.controller;
import com.example.eshop.ai.dto.*;
import com.example.eshop.ai.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/api/ai") @RequiredArgsConstructor
public class AiController {
    private final AiRouterService router;
    private final AiExecutionService history;
    @PostMapping("/execute")
    public ResponseEntity<AiResponse> execute(@Valid @RequestBody AiRequest request,
            @RequestHeader("Authorization") String bearer,
            @RequestHeader(value="Idempotency-Key", required=false) UUID key) {
        var result = router.execute(request, key == null ? UUID.randomUUID() : key, bearer);
        int status = switch(result.status()) {
            case SUCCESS -> 200; case RUNNING -> 202; case NEEDS_INPUT -> 422;
            case DENIED -> 403; case UNKNOWN -> 409; case FAILURE -> 502;
        };
        return ResponseEntity.status(status).body(result);
    }
    @GetMapping("/executions/{id}")
    public Object get(@PathVariable UUID id) {
        return history.existing(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
    }
}
