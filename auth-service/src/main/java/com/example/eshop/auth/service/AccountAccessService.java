package com.example.eshop.auth.service;

import com.example.eshop.auth.model.User;
import com.example.eshop.auth.repository.FunctionPermissionRepository;
import com.example.eshop.common.security.PermissionSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

/** Resolve current database roles and active functions for both AI and domain authorization. */
@Service @RequiredArgsConstructor
public class AccountAccessService {
    private final FunctionPermissionRepository functions;

    @Transactional(readOnly = true)
    public PermissionSnapshot snapshot(User user) {
        if (!user.isEnabled() || !"ACT".equals(user.getStatus()) || Boolean.TRUE.equals(user.getDeleted()))
            throw new AccessDeniedException("Account unavailable");
        boolean administrator = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> Set.of("ADMIN", "SUPER_ADMIN").contains(role.getName()));
        return new PermissionSnapshot(user.getId(), administrator,
                administrator ? functions.activeCodes() : functions.effectiveCodes(user.getId()));
    }
}
