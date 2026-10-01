package com.example.eshop.catalog.controller;

import com.example.eshop.catalog.config.SecurityConfig;
import com.example.eshop.catalog.service.impl.*;
import com.example.eshop.common.jwt.*;
import com.example.eshop.common.exception.GlobalExceptionHandler;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

@WebMvcTest(properties = "jwt.secret=66546A5744444446E5A7234743777217A25432A462D4A614E645267556B587032733576")
@ContextConfiguration(classes = {PromotionAdminController.class, PromotionController.class,
    CatalogCheckoutController.class, SecurityConfig.class, JwtProperties.class,
    JwtTokenValidator.class, GlobalExceptionHandler.class, com.example.eshop.common.audit.AuditSecurityHandlers.class, com.example.eshop.common.audit.AuditLogController.class,
    com.example.eshop.common.request.RequestIdFilter.class})
class PromotionSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired JwtProperties jwt;
    @MockitoBean com.example.eshop.common.audit.AuditLogService audit;
    @MockitoBean com.example.eshop.common.audit.AuditLogRepository audits;
    @MockitoBean PromotionService service;
    @MockitoBean CatalogCheckoutService checkout;

    private String token(String role, boolean enabled, boolean refresh) {
        return "Bearer " + Jwts.builder().setSubject("test").claim("authorities", List.of(role))
            .claim("isEnable", enabled).claim("type", refresh ? "refresh" : "access")
            .setExpiration(Date.from(Instant.now().plusSeconds(60)))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwt.getSecret()))).compact();
    }
    @Test void onlyAdminCanReadAdminDetails() throws Exception {
        mvc.perform(get("/api/v1/admin/promotions/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/promotions/1").header("Authorization",token("CUSTOMER",true,false)))
            .andExpect(status().isForbidden());
        verifyNoInteractions(service);
        mvc.perform(get("/api/v1/admin/promotions/1").header("Authorization",token("ADMIN",true,false)))
            .andExpect(status().isOk());
    }
    @Test void storefrontIsPublicAndInternalCheckoutRejectsEndUserTokens() throws Exception {
        mvc.perform(get("/api/v1/promotions/active")).andExpect(status().isOk());
        mvc.perform(post("/internal/catalog/checkouts/1/confirm").header("Authorization",token("ADMIN",true,false)))
            .andExpect(status().isForbidden());
        verifyNoInteractions(checkout);
        mvc.perform(post("/internal/catalog/checkouts/1/confirm").header("Authorization",token("SERVICE_ORDER",true,false)))
            .andExpect(status().isOk());
    }
    @Test void disabledAndRefreshTokensAreRejected() throws Exception {
        mvc.perform(get("/api/v1/admin/promotions/1").header("Authorization",token("ADMIN",false,false)))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/promotions/1").header("Authorization",token("ADMIN",true,true)))
            .andExpect(status().isUnauthorized());
    }
    @Test void invalidPromotionBodyReturns400() throws Exception {
        mvc.perform(post("/api/v1/admin/promotions").header("Authorization",token("ADMIN",true,false))
            .contentType("application/json").content("{\"name\":\" \"}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void auditSearchIsProtectedAndRequestIdIsReturned() throws Exception {
        mvc.perform(get("/api/admin/audit-logs").header("X-Request-ID", "req_denied"))
            .andExpect(status().isUnauthorized()).andExpect(header().string("X-Request-ID", "req_denied"))
            .andExpect(jsonPath("$.requestId").value("req_denied"));
        mvc.perform(get("/api/admin/audit-logs").header("Authorization", token("USER",true,false)))
            .andExpect(status().isForbidden());
        verifyNoInteractions(audits);
        mvc.perform(get("/api/admin/audit-logs").header("Authorization", token("ADMIN",true,false)))
            .andExpect(status().isOk());
        mvc.perform(get("/api/admin/audit-logs?size=1000").header("Authorization", token("ADMIN",true,false)))
            .andExpect(status().isBadRequest());
    }
}

