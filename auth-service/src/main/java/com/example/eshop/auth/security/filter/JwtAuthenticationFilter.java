package com.example.eshop.auth.security.filter;


import com.example.eshop.auth.jwt.JwtConfig;
import com.example.eshop.auth.jwt.JwtService;
import com.example.eshop.auth.security.UserDetailsImpl;
import com.example.eshop.auth.security.UserDetailsService;
import com.example.eshop.auth.dto.request.Login;
import com.example.eshop.auth.dto.response.AuthenticationResponse;
import com.example.eshop.auth.util.CustomMessageExceptionUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
public class JwtAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private final com.example.eshop.common.audit.AuditLogService audit;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final UserDetailsService customUserDetailService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   ObjectMapper objectMapper,
                                   JwtConfig jwtConfig,
                                   AuthenticationManager authenticationManager,
                                   UserDetailsService customUserDetailService,
                                   com.example.eshop.common.audit.AuditLogService audit) {
        super(jwtConfig.getUrl().startsWith("/") ? jwtConfig.getUrl() : "/" + jwtConfig.getUrl());
        setAuthenticationManager(authenticationManager);
        this.audit = audit;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.customUserDetailService = customUserDetailService;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {

        log.info("Start attempt to authentication");
        Login authenticationRequest = objectMapper.readValue(request.getInputStream(), Login.class);
        request.setAttribute("loginUsername", authenticationRequest.CriteriaValue());
        log.info("End attempt to authentication");

        try {
        return getAuthenticationManager()
                .authenticate(new UsernamePasswordAuthenticationToken(
                        authenticationRequest.CriteriaValue(),
                        authenticationRequest.Password(),
                        Collections.emptyList())
                );
        } catch (com.example.eshop.common.exception.CustomMessageException ex) {
            throw new org.springframework.security.authentication.BadCredentialsException("Authentication failed");
        }
    }

    @Override
    public void successfulAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain,
            Authentication authResult
    ) throws IOException {

        Object principal = authResult.getPrincipal();

        UserDetailsImpl userDetails;

        if (principal instanceof UserDetailsImpl) {
            userDetails = (UserDetailsImpl) principal;
        } else {
            String username = principal.toString();
            userDetails = (UserDetailsImpl) customUserDetailService.loadUserByUsername(username);
        }

        audit.recordSecurity(com.example.eshop.common.audit.AuditAction.LOGIN, com.example.eshop.common.audit.AuditResult.SUCCESS, authResult, null);
        customUserDetailService.updateAttempt(userDetails.getUsername());

        var accessToken = jwtService.generateToken(userDetails);
        var refreshToken = jwtService.refreshToken(userDetails);
        response.setHeader("Authorization", "Bearer " + accessToken);
        response.setHeader("Access-Control-Expose-Headers", "Authorization, X-Request-ID");
        AuthenticationResponse authenticationResponse =
                new AuthenticationResponse(null,accessToken, refreshToken);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter()
                .write(objectMapper.writeValueAsString(authenticationResponse));

        log.info("Successful Authentication - Token generated and added to response");

    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {
        audit.recordSecurity(com.example.eshop.common.audit.AuditAction.LOGIN_FAILED,
            com.example.eshop.common.audit.AuditResult.FAILURE, null, "AUTHENTICATION_FAILED");
        String usernameOrEmail = (String) request.getAttribute("loginUsername");
        if (usernameOrEmail != null && !usernameOrEmail.isBlank()) {
            customUserDetailService.saveUserAttemptAuthentication(usernameOrEmail);
        }
        var messageException = CustomMessageExceptionUtils.unauthorized();
        var msgJson = objectMapper.writeValueAsString(com.example.eshop.common.dto.APIResponse.error("Authentication failed", 401));
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(msgJson);
        log.info("Unsuccessful Authentication {}", failed.getLocalizedMessage());
    }
}