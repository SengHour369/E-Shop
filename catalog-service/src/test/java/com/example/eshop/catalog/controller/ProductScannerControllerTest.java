package com.example.eshop.catalog.controller;

import com.example.eshop.catalog.config.SecurityConfig;
import com.example.eshop.catalog.dto.request.ScanRequest;
import com.example.eshop.catalog.dto.response.ScanResponse;
import com.example.eshop.catalog.scanner.ScanRateLimitedException;
import com.example.eshop.catalog.service.impl.ProductScannerService;
import com.example.eshop.common.exception.GlobalExceptionHandler;
import com.example.eshop.common.jwt.JwtProperties;
import com.example.eshop.common.jwt.JwtTokenValidator;
import com.example.eshop.common.request.RequestIds;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(properties = "jwt.secret=66546A5744444446E5A7234743777217A25432A462D4A614E645267556B587032733576")
@ContextConfiguration(classes = {ProductScannerController.class, SecurityConfig.class, JwtProperties.class,
        JwtTokenValidator.class, GlobalExceptionHandler.class,
        com.example.eshop.common.audit.AuditSecurityHandlers.class,
        com.example.eshop.common.request.RequestIdFilter.class})
class ProductScannerControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JwtProperties jwt;
    @MockitoBean com.example.eshop.common.audit.AuditLogService audit;
    @MockitoBean ProductScannerService scanner;

    @Test void anonymousCallerIsRejectedAndTheRequestIdIsPreserved() throws Exception {
        mvc.perform(post("/api/v1/product-scanner/scan").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Request-ID", "req_denied").content("{\"code\":\"8850123456787\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Request-ID", "req_denied"))
                .andExpect(jsonPath("$.requestId").value("req_denied"));
        verify(scanner, never()).scan(any());
    }

    @Test void authenticatedCustomerScanPropagatesTheRequestId() throws Exception {
        when(scanner.scan(any())).thenAnswer(invocation -> {
            ScanRequest request = invocation.getArgument(0);
            return ScanResponse.builder().found(true).code(request.code()).message("Product found")
                    .requestId(RequestIds.current()).build();
        });
        mvc.perform(post("/api/v1/product-scanner/scan").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token("USER"))
                        .header("X-Request-ID", "req_scan_42")
                        .content("{\"code\":\"8850123456787\",\"format\":\"EAN_13\",\"originalPrice\":1,\"productId\":9}"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", "req_scan_42"))
                .andExpect(jsonPath("$.requestId").value("req_scan_42"))
                .andExpect(jsonPath("$.data.requestId").value("req_scan_42"))
                .andExpect(jsonPath("$.data.found").value(true))
                .andExpect(jsonPath("$.data.code").value("8850123456787"));
    }

    @Test void staffCanScanAndInvalidInputIsRejected() throws Exception {
        when(scanner.scan(any())).thenReturn(ScanResponse.builder().found(false).message("Product not found").build());
        mvc.perform(post("/api/v1/product-scanner/scan").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token("STAFF")).content("{\"code\":\"NIKE-BLK-42\",\"format\":\"SKU\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/product-scanner/scan").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token("ADMIN")).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/product-scanner/scan").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token("USER")).content("{\"code\":\"123\",\"format\":\"PDF417\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test void tooManyScansReturn429() throws Exception {
        when(scanner.scan(any())).thenThrow(new ScanRateLimitedException());
        mvc.perform(post("/api/v1/product-scanner/scan").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token("USER")).header("X-Request-ID", "req_limited")
                        .content("{\"code\":\"8850123456787\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "1"))
                .andExpect(jsonPath("$.requestId").value("req_limited"));
    }

    private String token(String role) {
        return "Bearer " + Jwts.builder().setSubject("scanner-user").claim("authorities", List.of(role))
                .claim("isEnable", true).claim("type", "access")
                .setExpiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwt.getSecret()))).compact();
    }
}
