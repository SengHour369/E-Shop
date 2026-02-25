package com.example.learning_spring_security.filter;

import com.example.learning_spring_security.JWT.JwtConfig;
import com.example.learning_spring_security.JWT.JwtService;
import com.example.learning_spring_security.Security.UserDetailsImpl;
import com.example.learning_spring_security.Security.UserDetailsService;
import com.example.learning_spring_security.dto.Request.Login;
import com.example.learning_spring_security.dto.Response.AuthenticationResponse;
import com.example.learning_spring_security.utils.CustomMessageExceptionUtils;
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

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final UserDetailsService customUserDetailService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   ObjectMapper objectMapper,
                                   JwtConfig jwtConfig,
                                   AuthenticationManager authenticationManager,
                                   UserDetailsService customUserDetailService) {
        super(jwtConfig.getUrl().startsWith("/") ? jwtConfig.getUrl() : "/" + jwtConfig.getUrl());
        setAuthenticationManager(authenticationManager);
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.customUserDetailService = customUserDetailService;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {

        log.info("Start attempt to authentication");
        Login authenticationRequest = objectMapper.readValue(request.getInputStream(), Login.class);

        customUserDetailService.saveUserAttemptAuthentication(authenticationRequest.username());
        log.info("End attempt to authentication");

        return getAuthenticationManager()
                .authenticate(new UsernamePasswordAuthenticationToken(
                        authenticationRequest.username(),
                        authenticationRequest.password(),
                        Collections.emptyList())
                );
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

        var accessToken = jwtService.generateToken(userDetails);
        var refreshToken = jwtService.refreshToken(userDetails);
        response.setHeader("Authorization", "Bearer " + accessToken);
        response.setHeader("Access-Control-Expose-Headers", "Authorization");
        AuthenticationResponse authenticationResponse =
                new AuthenticationResponse(accessToken, refreshToken);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter()
                .write(objectMapper.writeValueAsString(authenticationResponse));

        log.info("Successful Authentication - Token generated and added to response");
        log.info("Authorization Header: Bearer {}", accessToken);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {
        var messageException = CustomMessageExceptionUtils.unauthorized();
        var msgJson = objectMapper.writeValueAsString(messageException);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(msgJson);
        log.info("Unsuccessful Authentication {}", failed.getLocalizedMessage());
    }
}