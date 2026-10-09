package com.example.eshop.auth.controller;

import com.example.eshop.auth.repository.FunctionPermissionRepository;
import com.example.eshop.auth.repository.UserRepository;
import com.example.eshop.common.security.CurrentActor;
import com.example.eshop.common.security.PermissionSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequiredArgsConstructor
public class InternalAuthorizationController {

    private final UserRepository users;
    private final FunctionPermissionRepository functions;

    @GetMapping("/internal/authorization/me")
    @Transactional(readOnly = true)
    public PermissionSnapshot current(Authentication authentication) {
        long actorId = CurrentActor.userId();
        var user = users.findById(actorId)
                .filter(value -> value.isEnabled())
                .filter(value -> "ACT".equals(value.getStatus()))
                .filter(value -> !Boolean.TRUE.equals(value.getDeleted()))
                .filter(value -> value.getUsername().equals(authentication.getName()))
                .orElseThrow(() -> new AccessDeniedException("Account unavailable"));
        boolean administrator = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> Set.of("ADMIN", "SUPER_ADMIN").contains(role.getName()));
        return new PermissionSnapshot(actorId, administrator, functions.effectiveCodes(actorId));
    }

    @GetMapping("/internal/ai/users")
    @Transactional(readOnly = true)
    public java.util.List<UserCard> users(Authentication authentication) {
        PermissionSnapshot actor = current(authentication);
        if (!actor.administrator() || !actor.permits("USER_VIEW")) {
            throw new AccessDeniedException("Permission denied");
        }
        return users.findVisibleForAssistant(org.springframework.data.domain.PageRequest.of(0, 20,
                        org.springframework.data.domain.Sort.by("id").descending()))
                .stream()
                .map(user -> new UserCard(user.getId(), user.getUsername(), user.getStatus(),
                        user.isEnabled(), user.getCreatedAt()))
                .toList();
    }

    public record UserCard(Long id, String username, String status, boolean enabled,
            java.time.LocalDateTime createdAt) {
    }
}
