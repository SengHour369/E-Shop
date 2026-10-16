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
    private final com.example.eshop.ai.service.AiImageAttachment imageAttachment;
    private final com.fasterxml.jackson.databind.ObjectMapper mapper;
    private final com.example.eshop.ai.service.AiQuotaService quota;
    private final com.example.eshop.ai.service.AiAdminWorkspaceService workspace;
    private final com.example.eshop.ai.service.AiConversationService conversation;
    private final AiExecutionService history;
    private final com.example.eshop.ai.service.AiChatStateService chatState;
    private final com.example.eshop.common.security.LivePermissionService permissions;
    private final com.example.eshop.ai.service.AiChatPresenter presenter;

    @org.springframework.web.bind.annotation.ExceptionHandler(com.example.eshop.ai.service.AiFailure.class)
    public ResponseEntity<AiResponse> safeFailure(com.example.eshop.ai.service.AiFailure failure) {
        return ResponseEntity.status(httpStatus(failure.status())).body(new AiResponse(null,
                com.example.eshop.common.request.RequestIds.current(),org.slf4j.MDC.get("traceId"),
                com.example.eshop.ai.enums.AiIntent.PRODUCT_VISUAL_SEARCH,failure.status(),failure.getMessage(),failure.code(),null));
    }

    @PostMapping(value="/visual-search", consumes="multipart/form-data")
    public ResponseEntity<AiResponse> visual(
            @org.springframework.web.bind.annotation.RequestPart("image") org.springframework.web.multipart.MultipartFile image,
            @RequestHeader("Authorization") String bearer,
            @RequestHeader("Idempotency-Key") UUID key,
            jakarta.servlet.http.HttpServletRequest servlet) {
        chatState.rateLimit(servlet.getRemoteAddr()); permissions.current();
        String fingerprint = imageAttachment.fingerprint(image);
        var cached = quota.process(key,"visual:"+image.getContentType()+":"+fingerprint,AiResponse.class,() -> {
            UUID upload = UUID.randomUUID();
            var parameters=mapper.createObjectNode().put("uploadId",upload.toString());
            return imageAttachment.with(upload,image,() -> {
                var result=router.executeRegistered(com.example.eshop.ai.enums.AiIntent.PRODUCT_VISUAL_SEARCH,parameters,key,bearer);
                return ResponseEntity.status(httpStatus(result.status())).body(result);
            });
        });
        router.authorizeReplay(cached.getBody()); return cached;
    }

    @PostMapping("/tools/{tool}")
    public ResponseEntity<AiResponse> tool(
            @PathVariable com.example.eshop.ai.enums.AiIntent tool,
            @RequestBody com.fasterxml.jackson.databind.JsonNode parameters,
            @RequestHeader("Authorization") String bearer,
            @RequestHeader("Idempotency-Key") UUID key,
            @RequestHeader(value="X-Conversation-ID",required=false) UUID conversationId,
            jakarta.servlet.http.HttpServletRequest servlet) {
        chatState.rateLimit(servlet.getRemoteAddr());
        permissions.current();
        if (conversationId != null) chatState.conversation(conversationId);
        var cached = quota.processRegistered(key, "tool:" + tool + ":" + parameters, AiResponse.class,
                () -> {
                    var result = router.executeRegistered(tool, parameters, key, bearer);
                    return ResponseEntity.status(httpStatus(result.status())).body(result);
                });
        router.authorizeReplay(cached.getBody());
        if (conversationId != null && cached.getBody() != null) chatState.remember(conversationId,cached.getBody());
        return cached;
    }

    @PostMapping("/confirm/{id}")
    public ResponseEntity<AiResponse> confirm(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String bearer,
            jakarta.servlet.http.HttpServletRequest servlet
    ) {
        chatState.rateLimit(servlet.getRemoteAddr());
        permissions.current();
        AiResponse result = router.confirm(id, bearer);
        return ResponseEntity.status(httpStatus(result.status())).body(result);
    }

    @PostMapping({"/chat", "/admin/chat"})
    public ResponseEntity<com.example.eshop.ai.dto.AiChatResponse> chat(
            @Valid @RequestBody com.example.eshop.ai.dto.AiChatRequest request,
            @RequestHeader(value = "Authorization", required = false) String bearer,
            @RequestHeader(value = "Idempotency-Key", required = false) UUID key,
            jakarta.servlet.http.HttpServletRequest servlet
    ) {
        chatState.rateLimit(servlet.getRemoteAddr());
        boolean administrator = servlet.getRequestURI().endsWith("/admin/chat");

        if (bearer != null && !com.example.eshop.ai.registry.AiToolRegistry.authenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authentication"
            );
        }

        if (administrator && !permissions.current().administrator()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Administrator access required"
            );
        }

        if (com.example.eshop.ai.registry.AiToolRegistry.authenticated()) {
            permissions.current();
            if (request.conversationId() != null) chatState.conversation(request.conversationId());
            var cached = quota.process(
                    key == null ? UUID.randomUUID() : key,
                    "chat:" + administrator + ":" + request.toString(),
                    com.example.eshop.ai.dto.AiChatResponse.class,
                    () -> acceptedChat(request, bearer, administrator)
            );
            if (cached.getBody() != null) router.authorizeReplay(cached.getBody().result());
            return cached;
        }

        return acceptedChat(request, bearer, administrator);
    }

    private ResponseEntity<com.example.eshop.ai.dto.AiChatResponse> acceptedChat(
            com.example.eshop.ai.dto.AiChatRequest request,
            String bearer,
            boolean administrator
    ) {
        UUID conversationId = chatState.conversation(request.conversationId());

        if (!administrator && CustomerPurchaseGuide.matches(request.message())) {
            var guide = CustomerPurchaseGuide.response(
                    conversationId,
                    request.language()
            );
            chatState.rememberDialogue(
                    conversationId,
                    request.message(),
                    guide.result().message()
            );
            return ResponseEntity.ok(guide);
        }

        if (AiQuickReply.reply(request.message(), request.language()) != null
                || AiConversationRouting.isConversation(request.message())) {
            return conversational(conversationId, administrator, request);
        }

        String message = chatState.withContext(conversationId, request.message());
        AiResponse result = router.execute(
                new AiRequest(message == null ? request.message() : message),
                UUID.randomUUID(),
                bearer
        );

        if ("UNCERTAIN_INTENT".equals(result.errorCode())
                || "UNKNOWN_INTENT".equals(result.errorCode())
                || "MISSING_PARAMETER".equals(result.errorCode())
                || result.intent() == com.example.eshop.ai.enums.AiIntent.UNKNOWN
                && result.status() == AiExecutionStatus.NEEDS_INPUT) {
            return conversational(conversationId, administrator, request);
        }

        if (result.intent() == com.example.eshop.ai.enums.AiIntent.KNOWLEDGE_SEARCH
                && result.status() == AiExecutionStatus.SUCCESS
                && result.data() != null
                && "No approved knowledge is available.".equals(
                result.data().path("answer").asText())) {
            return conversational(conversationId, administrator, request);
        }

        chatState.remember(conversationId, result);
        var response = presenter.present(
                conversationId,
                administrator,
                request.language(),
                result
        );

        if (administrator) {
            workspace.remember(request.message(), response);
        } else {
            chatState.rememberDialogue(conversationId, request.message(), response.result().message());
        }

        return ResponseEntity.status(
                "AI_PROVIDER_BUSY".equals(result.errorCode())
                        ? HttpStatus.TOO_MANY_REQUESTS
                        : httpStatus(result.status())
        ).body(response);
    }

    private ResponseEntity<com.example.eshop.ai.dto.AiChatResponse> conversational(
            UUID id,
            boolean admin,
            com.example.eshop.ai.dto.AiChatRequest request
    ) {
        String answer;
        String errorCode = "AI_PROVIDER_FAILURE";
        String errorMessage = "The assistant could not respond. Please try again shortly.";

        try {
            answer = AiQuickReply.reply(request.message(), request.language());

            if (answer == null) {
                answer = conversation.reply(
                        request.message(),
                        request.language(),
                        chatState.dialogue(id)
                );
            }
        } catch (com.example.eshop.ai.service.AiFailure failure) {
            answer = null;
            errorCode = failure.code();
            errorMessage = failure.getMessage();
        }

        boolean success = answer != null;

        if (success) {
            chatState.rememberDialogue(id, request.message(), answer);
        }

        var result = new AiResponse(
                null,
                com.example.eshop.common.request.RequestIds.current(),
                null,
                com.example.eshop.ai.enums.AiIntent.UNKNOWN,
                success ? AiExecutionStatus.SUCCESS : AiExecutionStatus.FAILURE,
                success ? answer : errorMessage,
                success ? null : errorCode,
                null
        );
        var response = new com.example.eshop.ai.dto.AiChatResponse(
                id,
                admin ? "SUPER_ADMIN" : "CUSTOMER",
                success ? "ConversationCard" : "ErrorCard",
                java.util.List.of(),
                result
        );

        if (admin) {
            workspace.remember(request.message(), response);
        }

        return ResponseEntity.status(
                success
                        ? HttpStatus.OK
                        : "AI_PROVIDER_BUSY".equals(errorCode)
                        ? HttpStatus.TOO_MANY_REQUESTS
                        : HttpStatus.BAD_GATEWAY
        ).body(response);
    }

    @PostMapping("/execute")
    public ResponseEntity<AiResponse> execute(
            @Valid @RequestBody AiRequest request,
            @RequestHeader("Authorization") String bearer,
            @RequestHeader(value = "Idempotency-Key", required = false) UUID key,
            jakarta.servlet.http.HttpServletRequest servlet
    ) {
        chatState.rateLimit(servlet.getRemoteAddr());
        permissions.current();
        UUID executionId = key == null ? UUID.randomUUID() : key;
        var cached = quota.process(
                executionId,
                "execute:" + request.toString(),
                AiResponse.class,
                () -> {
                    AiResponse result = router.execute(
                            request,
                            executionId,
                            bearer
                    );
                    return ResponseEntity.status(
                            httpStatus(result.status())
                    ).body(result);
                }
        );
        router.authorizeReplay(cached.getBody());
        return cached;
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