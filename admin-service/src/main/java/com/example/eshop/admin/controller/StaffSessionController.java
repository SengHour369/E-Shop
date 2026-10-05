package com.example.eshop.admin.controller;

import com.example.eshop.admin.dto.StaffSessionResponse;
import com.example.eshop.admin.service.StaffSessionService;
import com.example.eshop.common.dto.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/session")
@RequiredArgsConstructor
public class StaffSessionController {

    private final StaffSessionService service;

    @GetMapping
    public APIResponse<StaffSessionResponse> current() {
        return APIResponse.success("Staff session", 200, service.current());
    }
}
