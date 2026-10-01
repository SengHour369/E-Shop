package com.example.eshop.common.audit;

import com.example.eshop.common.dto.APIResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class AuditSecurityHandlers {
    private final AuditLogService audit;
    private final ObjectMapper mapper;
    public void reject(HttpServletRequest request, HttpServletResponse response, int status) throws IOException {
        audit.recordSecurity(AuditAction.ACCESS_DENIED, AuditResult.DENIED,
                SecurityContextHolder.getContext().getAuthentication(), status == 401 ? "UNAUTHORIZED" : "FORBIDDEN");
        response.setStatus(status);
        response.setContentType("application/json");
        mapper.writeValue(response.getOutputStream(), APIResponse.error(status == 401 ? "Unauthorized" : "Forbidden", status));
    }
}
