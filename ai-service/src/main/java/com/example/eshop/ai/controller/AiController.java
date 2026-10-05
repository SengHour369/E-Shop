package com.example.eshop.ai.controller;

import com.example.eshop.ai.dto.AiRequest;
import com.example.eshop.ai.dto.AiResponse;
import com.example.eshop.ai.enums.AiExecutionStatus;
import com.example.eshop.ai.model.AiExecution;
import com.example.eshop.ai.service.AiExecutionService;
import com.example.eshop.ai.service.AiRouterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiRouterService router;
    private final AiExecutionService history;

    @PostMapping("/execute")
    public ResponseEntity<AiResponse> execute(@Valid @RequestBody AiRequest request,
            @RequestHeader("Authorization") String bearer,
            @RequestHeader(value = "Idempotency-Key", required = false) UUID key) {
        UUID executionId = key == null ? UUID.randomUUID() : key;
        AiResponse result = router.execute(request, executionId, bearer);
        return ResponseEntity.status(httpStatus(result.status())).body(result);
    }

    @GetMapping("/executions/{id}")
    public AiExecution execution(@PathVariable UUID id) {
        return history.existing(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static HttpStatus httpStatus(AiExecutionStatus status) {
        return switch (status) {
            case SUCCESS -> HttpStatus.OK;
            case RUNNING -> HttpStatus.ACCEPTED;
            case NEEDS_INPUT -> HttpStatus.UNPROCESSABLE_ENTITY;
            case DENIED -> HttpStatus.FORBIDDEN;
            case UNKNOWN -> HttpStatus.CONFLICT;
            case FAILURE -> HttpStatus.BAD_GATEWAY;
        };
    }
}
