package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Model.User;
import com.example.learning_spring_security.Repository.UserRepository;
import com.example.learning_spring_security.Service.ServiceImplement.AuthServiceImpl;
import com.example.learning_spring_security.dto.Request.*;
import com.example.learning_spring_security.dto.Response.AuthenticationResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import com.example.learning_spring_security.dto.Request.VerifyUserDto;
import com.example.learning_spring_security.JWT.JwtService;
import com.example.learning_spring_security.Security.UserDetailsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final AuthServiceImpl authService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<ResponseErrorTemplate> register(@Valid @RequestBody Register request) {
        log.info("Register request for: {}", request.username());
        ResponseErrorTemplate response = authService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<AuthenticationResponse> verify(
            @RequestParam String email,
            @RequestParam String code) {

        log.info("Verify endpoint called for email: {}", email);

        try {
            VerifyUserDto verifyUserDto = new VerifyUserDto();
            verifyUserDto.setEmail(email);
            verifyUserDto.setVerificationCode(code);

            AuthenticationResponse response = authService.verifyUser(verifyUserDto);

            return ResponseEntity.ok(AuthenticationResponse.builder()
                    .id(response.id())
                    .accessToken(null)
                    .refreshToken(null)
                    .tokenType("Bearer")
                    .email(response.email())
                    .username(response.username())
                    .role(response.role())
                    .message("Email verified successfully. You can now log in.")
                    .build());

        } catch (Exception e) {
            log.error("Verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AuthenticationResponse.builder()
                            .message(e.getMessage())
                            .build());
        }
    }

    @PostMapping("/resend")
    public ResponseEntity<AuthenticationResponse> resend(@RequestParam String email) {
        log.info("Resend verification code to: {}", email);
        AuthenticationResponse response = authService.resendVerificationCode(email);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/email/username/login")
    public ResponseEntity<AuthenticationResponse> loginByEmail(@Valid @RequestBody Login request) {
        log.info("Login request with criteria type: {}"
                , request.CriteriaValue());

        try {
            AuthenticationResponse response = authService.authenticate(request);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthenticationResponse.builder()
                            .message(e.getMessage())
                            .build());
        }
    }


    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Refresh token request");
        try {
            AuthenticationResponse response = authService.refreshToken(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthenticationResponse.builder().message(e.getMessage()).build());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthenticationResponse> logout(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Logout request");
        try {
            AuthenticationResponse response = authService.logout(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AuthenticationResponse.builder().message(e.getMessage()).build());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<AuthenticationResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot password request for email: {}", request.getEmail());
        try {
            AuthenticationResponse response = authService.forgotPassword(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AuthenticationResponse.builder().message(e.getMessage()).build());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthenticationResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset password request");
        try {
            AuthenticationResponse response = authService.resetPassword(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AuthenticationResponse.builder().message(e.getMessage()).build());
        }
    }

    @PostMapping("/check-user")
    public ResponseEntity<AuthenticationResponse> checkUserExists(@RequestParam String email) {
        log.info("Checking if user exists: {}", email);
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            return ResponseEntity.ok(AuthenticationResponse.builder()
                    .id(user.get().getId())
                    .email(user.get().getEmail())
                    .username(user.get().getUsername())
                    .build());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AuthenticationResponse.builder().message("User not found").build());
        }
    }
}