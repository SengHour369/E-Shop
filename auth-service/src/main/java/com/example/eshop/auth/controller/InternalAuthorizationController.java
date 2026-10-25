package com.example.eshop.auth.controller;

import com.example.eshop.auth.service.AccountAccessService;
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
    private final AccountAccessService access;
    private final com.example.eshop.auth.repository.WorkflowGrantRepository workflowGrants;

    @GetMapping({"/internal/authorization/me", "/api/v1/user/access"})
    @Transactional(readOnly = true)
    public PermissionSnapshot current(Authentication authentication) {
        long actorId = CurrentActor.userId();
        var user = users.findById(actorId)
                .filter(value -> value.isEnabled())
                .filter(value -> "ACT".equals(value.getStatus()))
                .filter(value -> !Boolean.TRUE.equals(value.getDeleted()))
                .filter(value ->
                        value.getUsername().equals(authentication.getName())
                )
                .orElseThrow(() -> new AccessDeniedException(
                        "Account unavailable"
                ));
        if(authentication.getDetails() instanceof java.util.Map<?,?> claims && claims.get("workflowGrantId") instanceof String id){
            var grant=workflowGrants.findById(java.util.UUID.fromString(id)).filter(g->g.getOwnerId()==actorId&&!g.isRevoked()&&g.getExpiresAt().isAfter(java.time.Instant.now())&&g.getVersionId().toString().equals(claims.get("workflowVersionId"))).orElseThrow(()->new AccessDeniedException("Workflow authorization expired or revoked"));
        }
        return access.snapshot(user);
    }

    @GetMapping("/internal/authorization/users/{id}/active")
    @Transactional(readOnly = true)
    public boolean active(
            @org.springframework.web.bind.annotation.PathVariable long id,
            Authentication authentication
    ) {
        var actor = current(authentication);

        if (!actor.administrator() || !actor.permits("BRANCH_ACCESS_MANAGE")) {
            throw new AccessDeniedException(
                    "Branch access administrator required"
            );
        }

        return users.findById(id)
                .filter(u ->
                        u.isEnabled()
                                && "ACT".equals(u.getStatus())
                                && !Boolean.TRUE.equals(u.getDeleted())
                )
                .isPresent();
    }

    @GetMapping("/internal/ai/users")
    @Transactional(readOnly = true)
    public java.util.List<UserCard> users(Authentication authentication) {
        PermissionSnapshot actor = current(authentication);

        if (!actor.administrator() || !actor.permits("USER_VIEW")) {
            throw new AccessDeniedException("Permission denied");
        }

        return users.findVisibleForAssistant(
                        org.springframework.data.domain.PageRequest.of(
                                0,
                                20,
                                org.springframework.data.domain.Sort
                                        .by("id")
                                        .descending()
                        )
                )
                .stream()
                .map(user -> new UserCard(
                        user.getId(),
                        user.getUsername(),
                        user.getStatus(),
                        user.isEnabled(),
                        user.getCreatedAt()
                ))
                .toList();
    }

    public record UserCard(
            Long id,
            String username,
            String status,
            boolean enabled,
            java.time.LocalDateTime createdAt
    ) {
    }
}