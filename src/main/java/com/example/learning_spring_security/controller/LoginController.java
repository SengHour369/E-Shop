package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.dto.AuthenticationRequest;
import com.example.learning_spring_security.dto.AuthenticationResponse;
import com.example.learning_spring_security.Security.UserDetailsImpl;
import com.example.learning_spring_security.JWT.JwtService;
import com.example.learning_spring_security.Security.UserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody AuthenticationRequest authenticationRequest) {
        log.info("Login attempt for user: {}", authenticationRequest.username());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authenticationRequest.username(),
                            authenticationRequest.password()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Get user details safely

            Object principal = authentication.getPrincipal();
            UserDetailsImpl userDetails;

            if (principal instanceof UserDetailsImpl) {
                userDetails = (UserDetailsImpl) principal;
            } else {

                // Fallback: load from service if principal is String

                String username = principal.toString();
                userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(username);
            }

            // Reset attempt counter on successful login

            userDetailsService.updateAttempt(authenticationRequest.username());

            // Generate tokens

            String accessToken = jwtService.generateToken(userDetails);
            String refreshToken = jwtService.refreshToken(userDetails);

            log.info("User {} logged in successfully", authenticationRequest.username());

            return ResponseEntity.ok(new AuthenticationResponse(accessToken, refreshToken));

        } catch (BadCredentialsException e) {
            log.error("Invalid credentials for user: {}", authenticationRequest.username());
            // Track failed attempt
            userDetailsService.saveUserAttemptAuthentication(authenticationRequest.username());
            throw new BadCredentialsException("Invalid username or password");
        }
    }

}