package com.example.eshop.ai.controller;

import com.example.eshop.ai.service.AiAdminWorkspaceService;
import com.example.eshop.common.security.LivePermissionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/ai/admin/conversations")
@RequiredArgsConstructor
public class AiAdminWorkspaceController {
    private final AiAdminWorkspaceService workspace;
    private final LivePermissionService permissions;
    private void requireAdmin() {
        if (!permissions.current().administrator()) throw new org.springframework.security.access.AccessDeniedException("Administrator access required");
    }
    @GetMapping public List<AiAdminWorkspaceService.Summary> list() { requireAdmin(); return workspace.list(); }
    @GetMapping("/{id}") public AiAdminWorkspaceService.Transcript get(@PathVariable UUID id) { requireAdmin(); return workspace.get(id); }
    public record Rename(@NotBlank @Size(max = 80) String title) {}
    @PatchMapping("/{id}") public AiAdminWorkspaceService.Transcript rename(@PathVariable UUID id, @Valid @RequestBody Rename request) {
        requireAdmin(); return workspace.rename(id, request.title());
    }
}
