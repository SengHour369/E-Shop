package com.example.eshop.ai.controller;

import com.example.eshop.ai.service.AiConversationMemory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/ai/admin/memory")
@RequiredArgsConstructor
public class AiMemoryController {

    private final AiConversationMemory memory;

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(memory.list(page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> transcript(
            @PathVariable UUID id,
            @RequestParam long actorId,
            @RequestParam String scope,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(memory.transcript(id, actorId, scope, page));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable UUID id,
            @RequestParam long actorId,
            @RequestParam String scope,
            @RequestParam(defaultValue = "false") boolean confirm
    ) {
        if (!confirm) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Explicit confirm=true is required to delete saved memory."
            );
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of(
                        "deleted",
                        memory.delete(id, actorId, scope)
                ));
    }
}