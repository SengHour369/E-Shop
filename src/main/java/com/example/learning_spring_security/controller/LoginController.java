package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Exception.CustomMessageException;
import com.example.learning_spring_security.JWT.JwtService;
import com.example.learning_spring_security.Model.User;
import com.example.learning_spring_security.Repository.UserRepository;
import com.example.learning_spring_security.Service.ServiceImplement.AuthServiceImpl;
import com.example.learning_spring_security.dto.Request.*;
import com.example.learning_spring_security.dto.Response.AuthenticationResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import com.example.learning_spring_security.dto.Request.VerifyUserDto;
import com.example.learning_spring_security.Security.UserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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
                    .accessToken(response.accessToken())
                    .refreshToken(response.refreshToken())
                    .tokenType("Bearer")
                    .email(response.email())
                    .username(response.username())
                    .role(response.role())
                    .message("Email verified successfully.")
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
        log.info("Reset password request = {}", request.Password());

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
            return ResponseEntity.status(statusOf(e))
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
            return ResponseEntity.status(statusOf(e))
                    .body(AuthenticationResponse.builder().message(e.getMessage()).build());
        }
    }

    /**
     * Reads the reset-password token from the emailed link and lets the user submit a
     * new password directly, since the API's /reset-password endpoint needs a JSON
     * body (token + newPassword) that a plain link click can never supply.
     */
    @GetMapping("/reset-password-page")
    public void resetPasswordPage(@RequestParam String token, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write("""
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8" />
                  <title>Reset Password</title>
                  <style>
                    body { font-family: Arial, sans-serif; background: #f4f6f8; display: flex; justify-content: center; padding-top: 60px; }
                    .card { background: #fff; padding: 32px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.08); width: 100%; max-width: 360px; }
                    h2 { margin-top: 0; color: #333; }
                    input { width: 100%; padding: 10px; margin: 8px 0; border: 1px solid #ccc; border-radius: 4px; box-sizing: border-box; }
                    button { width: 100%; padding: 12px; background: #2196F3; color: #fff; border: none; border-radius: 4px; cursor: pointer; font-size: 15px; }
                    button:disabled { background: #90caf9; cursor: not-allowed; }
                    #message { margin-top: 14px; font-size: 14px; }
                    .error { color: #d32f2f; }
                    .success { color: #2e7d32; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <h2>Reset your password</h2>
                    <form id="resetForm">
                      <input type="password" id="newPassword" placeholder="New password" minlength="6" required />
                      <input type="password" id="confirmPassword" placeholder="Confirm new password" minlength="6" required />
                      <button type="submit" id="submitBtn">Reset Password</button>
                    </form>
                    <div id="message"></div>
                  </div>
                  <script>
                    var token = new URLSearchParams(window.location.search).get('token');
                    var form = document.getElementById('resetForm');
                    var messageEl = document.getElementById('message');
                    var submitBtn = document.getElementById('submitBtn');

                    form.addEventListener('submit', function (e) {
                      e.preventDefault();
                      var newPassword = document.getElementById('newPassword').value;
                      var confirmPassword = document.getElementById('confirmPassword').value;

                      if (newPassword !== confirmPassword) {
                        messageEl.textContent = 'Passwords do not match.';
                        messageEl.className = 'error';
                        return;
                      }
                      if (!token) {
                        messageEl.textContent = 'Missing reset token.';
                        messageEl.className = 'error';
                        return;
                      }

                      submitBtn.disabled = true;
                      messageEl.textContent = '';
                      fetch('/api/v1/public/reset-password', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ token: token, newPassword: newPassword })
                      })
                        .then(function (res) {
                          return res.json().then(function (data) {
                            if (res.ok) {
                              messageEl.textContent = 'Password reset successful. You can now log in with your new password.';
                              messageEl.className = 'success';
                              form.style.display = 'none';
                            } else {
                              messageEl.textContent = data.message || 'Failed to reset password.';
                              messageEl.className = 'error';
                              submitBtn.disabled = false;
                            }
                          });
                        })
                        .catch(function () {
                          messageEl.textContent = 'Network error. Please try again.';
                          messageEl.className = 'error';
                          submitBtn.disabled = false;
                        });
                    });
                  </script>
                </body>
                </html>
                """);
    }

    private HttpStatus statusOf(Exception e) {
        if (e instanceof CustomMessageException cme) {
            try {
                return HttpStatus.valueOf(Integer.parseInt(cme.getCode()));
            } catch (IllegalArgumentException ignored) {
                // fall through to default below
            }
        }
        return HttpStatus.BAD_REQUEST;
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