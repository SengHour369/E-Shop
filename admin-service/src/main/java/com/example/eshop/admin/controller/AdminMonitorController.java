package com.example.eshop.admin.controller;

import com.example.eshop.admin.dto.MonitorStatusResponse;
import com.example.eshop.admin.service.AdminMonitorService;
import com.example.eshop.common.dto.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/monitor")
@RequiredArgsConstructor
public class AdminMonitorController {

    private final AdminMonitorService service;

    @GetMapping
    public APIResponse<MonitorStatusResponse> current() {
        return APIResponse.success("Admin monitor", 200, service.current());
    }
}
