package com.example.eshop.auth.controller;

import com.example.eshop.auth.repository.ApiPermissionRepository;
import com.example.eshop.auth.repository.UserRepository;
import com.example.eshop.auth.repository.FunctionPermissionRepository;
import com.example.eshop.auth.service.AccountAccessService;
import com.example.eshop.common.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class InternalApiAccessController {
    private final UserRepository users;
    private final AccountAccessService access;
    private final ApiPermissionRepository apis;
    private final FunctionPermissionRepository functions;

    @GetMapping("/internal/authorization/api-access")
    @Transactional(readOnly = true)
    public boolean allowed(Authentication authentication, @RequestParam String method, @RequestParam String path) {
        var user = users.findById(CurrentActor.userId())
                .filter(u -> u.getUsername().equals(authentication.getName()))
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Account unavailable"));
        var snapshot = access.snapshot(user);
        if (!snapshot.administrator()) return false;
        var matcher = new AntPathMatcher();
        return apis.findActiveByMethod(method).stream()
                .filter(api -> matcher.match(api.getApi(), path))
                .sorted(java.util.Comparator.comparing(com.example.eshop.auth.model.ApiPermission::getApi, matcher.getPatternComparator(path)))
                .findFirst()
                .map(api -> functions.findById(api.getFuncId())
                        .map(f -> snapshot.permits(f.getFuncCode())).orElse(false))
                .orElse(false);
    }
}
