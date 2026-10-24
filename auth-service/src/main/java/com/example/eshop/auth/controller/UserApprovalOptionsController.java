package com.example.eshop.auth.controller;
import com.example.eshop.auth.service.UserApprovalRouting;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequiredArgsConstructor
public class UserApprovalOptionsController {
    private final UserApprovalRouting routing;
    @GetMapping("/api/v1/user/approval-options")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> options(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(routing.currentOptions());}
}
