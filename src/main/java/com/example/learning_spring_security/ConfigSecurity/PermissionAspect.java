package com.example.learning_spring_security.ConfigSecurity;

import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.User;
import com.example.learning_spring_security.Repository.GroupPermissionRepository;
import com.example.learning_spring_security.Repository.UserRepository;
import com.example.learning_spring_security.Security.RequirePermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final GroupPermissionRepository userPermissionRepository;
    private final UserRepository userRepository;

    @Before("@annotation(requirePermission)")
    public void checkPermission(RequirePermission requirePermission) {
        long funcId = requirePermission.funcId();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        boolean hasPermission = userPermissionRepository.existsByGroupIdAndFuncIdAndIsActive(user.getId(), funcId, true);

        if (!hasPermission) {
            log.warn("Access denied: user '{}' (id={}) does not have permission for funcId={}",
                    username, user.getId(), funcId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not have permission to perform this action");
        }

        log.info("Permission granted: user '{}' (id={}) has funcId={}", username, user.getId(), funcId);
    }
}