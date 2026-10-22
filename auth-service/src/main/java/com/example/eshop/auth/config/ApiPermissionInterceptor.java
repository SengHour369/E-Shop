package com.example.eshop.auth.config;

import com.example.eshop.auth.exception.ForbiddenException;
import com.example.eshop.auth.exception.ResourceNotFoundException;
import com.example.eshop.auth.exception.UnauthorizedException;
import com.example.eshop.auth.model.ApiPermission;
import com.example.eshop.auth.model.User;
import com.example.eshop.auth.model.UserGroup;
import com.example.eshop.auth.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiPermissionInterceptor implements HandlerInterceptor {

    private final ApiPermissionRepository apiPermissionRepository;
    private final FunctionPermissionRepository functionPermissions;
    private final UserPermissionRepository userPermissionRepository;
    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;          // inject
    private final GroupPermissionRepository groupPermissionRepository; // inject

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private static final String SUPER_ADMIN_AUTHORITY = "ADMIN";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (isPublicPath(path)) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }

        String username = authentication.getName();

        if (isSuperAdmin(authentication)) {
            log.info("Permission bypass: super admin '{}' [{} {}]", username, method, path);
            return true;
        }

        ApiPermission api = apiPermissionRepository.findActiveByMethod(method).stream()
                .filter(candidate -> pathMatcher.match(candidate.getApi(), path))
                .sorted(java.util.Comparator.comparing(ApiPermission::getApi, pathMatcher.getPatternComparator(path)))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException(
                        "API not configured for permission check: " + method + " " + path));

        Long funcId = api.getFuncId();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        // ======== ពិនិត្យសិទ្ធិ (ទាំង User Permission និង Group Permission) ========
        boolean hasPermission = api.getFuncId() != null
                && functionPermissions.findById(api.getFuncId())
                    .map(function -> functionPermissions.effectiveCodes(user.getId()).contains(function.getFuncCode()))
                    .orElse(false);

        if (!hasPermission) {
            log.warn("Access denied: user '{}' (id={}) does not have direct or group permission for funcId={} [{} {}]",
                    username, user.getId(), funcId, method, path);
            throw new ForbiddenException("You do not have permission to perform this action");
        }

        log.info("Permission granted: user '{}' (id={}) funcId={} [{} {}]", username, user.getId(), funcId, method, path);
        return true;
    }

    private boolean isSuperAdmin(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .map(user -> user.getRoles().stream().anyMatch(role ->
                        java.util.Set.of("ADMIN", "SUPER_ADMIN").contains(role.getName())))
                .orElse(false);
    }

    private boolean isPublicPath(String path) {
        return Arrays.stream(SecurityConfig.PUBLIC_PATHS)
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}