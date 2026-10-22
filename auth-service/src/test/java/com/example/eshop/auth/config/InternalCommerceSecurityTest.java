package com.example.eshop.auth.config;

import com.example.eshop.auth.controller.CommerceAddressController;
import com.example.eshop.auth.repository.AddressRepository;
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

@WebMvcTest(CommerceAddressController.class)
@org.springframework.test.context.ContextConfiguration(classes = {CommerceAddressController.class, AppConfig.class, InternalCommerceSecurityConfig.class, InternalCommerceSecurityTest.JwtTestConfig.class})
class InternalCommerceSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired JwtProperties jwt;
    @MockitoBean AddressRepository addresses;
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

    @Test void signedOrderIdentityCanVerifyOwnershipWithoutCustomerAccount() throws Exception {
        when(addresses.isUserHasAddress(1L, 1L)).thenReturn(true);
        mvc.perform(get("/internal/commerce/addresses/1/owner/1")
                .header("Authorization", ServiceIdentity.bearer(jwt, "order-service", "SERVICE_ORDER")))
                .andExpect(status().isOk()).andExpect(content().string("true"));
        verify(addresses).isUserHasAddress(1L, 1L);
        verifyNoInteractions(customerPermissions);
    }
    @Test void nonOwnedAddressReturnsFalse() throws Exception {
        mvc.perform(get("/internal/commerce/addresses/2/owner/1")
                .header("Authorization", ServiceIdentity.bearer(jwt, "order-service", "SERVICE_ORDER")))
                .andExpect(status().isOk()).andExpect(content().string("false"));
    }
    @Test void customerAndOtherServiceCannotReadOwnership() throws Exception {
        for (String authority : new String[]{"ADMIN", "SERVICE_PAYMENT"}) {
            mvc.perform(get("/internal/commerce/addresses/1/owner/1")
                    .header("Authorization", ServiceIdentity.bearer(jwt, "caller", authority)));
        }
        verify(handlers, times(2)).reject(any(), any(), eq(403));
        verifyNoInteractions(addresses);
    }
    @Test void missingOrInvalidSignatureIsRejected() throws Exception {
        mvc.perform(get("/internal/commerce/addresses/1/owner/1"));
        var other = new JwtProperties();
        org.springframework.test.util.ReflectionTestUtils.setField(other, "secret", java.util.Base64.getEncoder().encodeToString("different-test-key-32-bytes-long!!".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        mvc.perform(get("/internal/commerce/addresses/1/owner/1")
                .header("Authorization", ServiceIdentity.bearer(other, "order-service", "SERVICE_ORDER")));
        verify(handlers, times(2)).reject(any(), any(), eq(401));
        verifyNoInteractions(addresses);
    }
}
