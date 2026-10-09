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
    private final com.example.eshop.ai.service.AiChatStateService chatState;
    private final com.example.eshop.common.security.LivePermissionService permissions;
    private final com.example.eshop.ai.service.AiChatPresenter presenter;

    @PostMapping("/confirm/{id}")
    public ResponseEntity<AiResponse> confirm(@PathVariable UUID id,
            @RequestHeader("Authorization") String bearer,
            jakarta.servlet.http.HttpServletRequest servlet) {
        chatState.rateLimit(servlet.getRemoteAddr());
        AiResponse result = router.confirm(id, bearer);
        return ResponseEntity.status(httpStatus(result.status())).body(result);
    }

    @PostMapping({"/chat", "/admin/chat"})
    public ResponseEntity<com.example.eshop.ai.dto.AiChatResponse> chat(
            @Valid @RequestBody com.example.eshop.ai.dto.AiChatRequest request,
            @RequestHeader(value = "Authorization", required = false) String bearer,
            jakarta.servlet.http.HttpServletRequest servlet) {
        chatState.rateLimit(servlet.getRemoteAddr());
        boolean administrator = servlet.getRequestURI().endsWith("/admin/chat");
        if (bearer != null && !com.example.eshop.ai.registry.AiToolRegistry.authenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication");
        }
        if (administrator && !permissions.current().administrator()) {
            throw new org.springframework.security.access.AccessDeniedException("Administrator access required");
        }
        UUID conversationId = chatState.conversation(request.conversationId());
        String message = chatState.withContext(conversationId, request.message());
        AiResponse result = router.execute(new AiRequest(message == null ? request.message() : message), UUID.randomUUID(), bearer);
        chatState.remember(conversationId, result);
        var response = presenter.present(conversationId, administrator, request.language(), result);
        return ResponseEntity.status(httpStatus(result.status())).body(response);
    }

    @PostMapping("/execute")
    public ResponseEntity<AiResponse> execute(@Valid @RequestBody AiRequest request,
            @RequestHeader("Authorization") String bearer,
            @RequestHeader(value = "Idempotency-Key", required = false) UUID key,
            jakarta.servlet.http.HttpServletRequest servlet) {
        chatState.rateLimit(servlet.getRemoteAddr());
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
