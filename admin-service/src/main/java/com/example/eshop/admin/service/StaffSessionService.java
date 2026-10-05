package com.example.eshop.admin.service;

import com.example.eshop.admin.dto.StaffSessionResponse;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class StaffSessionService {

    public StaffSessionResponse current() {
        Authentication authentication = requireStaff();
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return new StaffSessionResponse(authentication.getName(), authorities);
    }

    public Authentication requireStaff() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new AccessDeniedException("Sign in is required");
        }
        boolean staff = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ADMIN".equals(authority) || "MANAGER".equals(authority));
        if (!staff) {
            throw new AccessDeniedException("Staff access is required");
        }
        return authentication;
    }
}
