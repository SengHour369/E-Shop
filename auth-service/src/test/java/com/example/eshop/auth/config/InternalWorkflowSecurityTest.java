package com.example.eshop.auth.config;

import com.example.eshop.auth.controller.WorkflowGrantController;
import com.example.eshop.auth.repository.WorkflowGrantRepository;
import com.example.eshop.common.audit.AuditSecurityHandlers;
import com.example.eshop.common.automation.ServiceIdentity;
import com.example.eshop.common.jwt.JwtProperties;
import com.example.eshop.common.jwt.JwtTokenValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkflowGrantController.class)
@org.springframework.test.context.ContextConfiguration(classes = {WorkflowGrantController.class, AppConfig.class, InternalWorkflowSecurityConfig.class, InternalWorkflowSecurityTest.JwtTestConfig.class})
class InternalWorkflowSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired JwtProperties jwt;
    @MockitoBean WorkflowGrantRepository addresses;
    @MockitoBean ApiPermissionInterceptor customerPermissions;
    @MockitoBean AuditSecurityHandlers handlers;
    @MockitoBean com.example.eshop.common.audit.AuditLogService audit;

    @TestConfiguration
    static class JwtTestConfig {
        @Bean JwtProperties jwtProperties() {
            var p = new JwtProperties();
            org.springframework.test.util.ReflectionTestUtils.setField(p, "secret", java.util.Base64.getEncoder().encodeToString("test-only-signing-key-32-bytes-long".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            org.springframework.test.util.ReflectionTestUtils.setField(p, "header", "Authorization");
            org.springframework.test.util.ReflectionTestUtils.setField(p, "prefix", "Bearer");
            return p;
        }
        @Bean JwtTokenValidator tokenValidator(JwtProperties p) { return new JwtTokenValidator(p); }
    }


    @org.springframework.test.context.bean.override.mockito.MockitoBean com.example.eshop.auth.repository.UserRepository users;
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.example.eshop.auth.service.AccountAccessService access;
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.example.eshop.auth.jwt.JwtService signing;
    @Test void onlyWorkflowServiceIdentityCanExchangeGrants() throws Exception {
        for(String role:new String[]{"ADMIN","WORKFLOW_READ","SERVICE_PAYMENT"}) {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/internal/workflow-authorization/token")
              .contentType("application/json").content("{\"grantId\":\"00000000-0000-4000-8000-000000000001\",\"versionId\":\"00000000-0000-4000-8000-000000000002\"}")
              .header("Authorization",ServiceIdentity.bearer(jwt,"caller",role)));
        }
        verify(handlers,times(3)).reject(any(),any(),eq(403));verifyNoInteractions(addresses,users,signing);
    }
    @Test void workflowServicePassesSecurityAndChecksTheLiveGrant() throws Exception {
        when(addresses.findById(any())).thenReturn(java.util.Optional.empty());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/internal/workflow-authorization/token")
          .contentType("application/json").content("{\"grantId\":\"00000000-0000-4000-8000-000000000001\",\"versionId\":\"00000000-0000-4000-8000-000000000002\"}")
          .header("Authorization",ServiceIdentity.bearer(jwt,"ai-service","SERVICE_AI_WORKFLOW")));
        verify(addresses).findById(any());verifyNoInteractions(customerPermissions,users,signing);
    }
}
