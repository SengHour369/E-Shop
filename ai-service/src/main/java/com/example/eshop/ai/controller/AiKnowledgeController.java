package com.example.eshop.ai.controller;

import com.example.eshop.ai.service.AiKnowledgeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AiKnowledgeController {

    private final AiKnowledgeService knowledge;

    @org.springframework.web.bind.annotation.PutMapping("/api/ai/knowledge/{id}")
    public Map<String, UUID> replace(@org.springframework.web.bind.annotation.PathVariable UUID id,
            @Valid @RequestBody ApprovedDocument document) {
        return Map.of("documentId", knowledge.replace(id, document.title(), document.language(), document.content()));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/api/ai/knowledge/{id}")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void revoke(@org.springframework.web.bind.annotation.PathVariable UUID id) {
        knowledge.revoke(id);
    }

    @PostMapping("/api/ai/knowledge")
    public Map<String, UUID> approve(@Valid @RequestBody ApprovedDocument document) {
        return Map.of("documentId", knowledge.approve(document.title(), document.language(), document.content()));
    }

    public record ApprovedDocument(
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Pattern(regexp = "en|km") String language,
            @NotBlank @Size(max = 20000) String content) {
    }
}
