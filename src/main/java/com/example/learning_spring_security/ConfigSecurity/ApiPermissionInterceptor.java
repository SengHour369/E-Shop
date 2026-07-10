package com.example.learning_spring_security.ConfigSecurity;

import com.example.learning_spring_security.Exception.ExceptionService.ForbiddenException;
import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Exception.ExceptionService.UnauthorizedException;
import com.example.learning_spring_security.Model.ApiPermission;
import com.example.learning_spring_security.Model.User;
import com.example.learning_spring_security.Repository.ApiPermissionRepository;
import com.example.learning_spring_security.Repository.UserPermissionRepository;
import com.example.learning_spring_security.Repository.UserRepository;
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

/**
 * Resolves the funcId required for the current request from {@link ApiPermission}
 * (method + URL pattern) instead of a hardcoded @RequirePermission annotation, then
 * checks it against the authenticated user's {@code UserPermission} rows.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiPermissionInterceptor implements HandlerInterceptor {

    private final ApiPermissionRepository apiPermissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final UserRepository userRepository;

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

        // Super admin bypasses funcId resolution entirely — works even on endpoints
        // that have no api_permissions row yet (e.g. newly added modules).
        if (isSuperAdmin(authentication)) {
            log.info("Permission bypass: super admin '{}' [{} {}]", username, method, path);
            return true;
        }

        ApiPermission api = apiPermissionRepository.findActiveByMethod(method).stream()
                .filter(candidate -> pathMatcher.match(candidate.getApi(), path))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException(
                        "API not configured for permission check: " + method + " " + path));

        Long funcId = api.getFuncId();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        boolean hasPermission = userPermissionRepository.existsByUserIdAndFuncIdAndIsActive(user.getId(), funcId, true);
        if (!hasPermission) {
            log.warn("Access denied: user '{}' (id={}) does not have permission for funcId={} [{} {}]",
                    username, user.getId(), funcId, method, path);
            throw new ForbiddenException("You do not have permission to perform this action");
        }

        log.info("Permission granted: user '{}' (id={}) funcId={} [{} {}]", username, user.getId(), funcId, method, path);
        return true;
    }

    private boolean isSuperAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> SUPER_ADMIN_AUTHORITY.equals(authority.getAuthority()));
    }

    private boolean isPublicPath(String path) {
        return Arrays.stream(SecurityConfig.PUBLIC_PATHS)
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}