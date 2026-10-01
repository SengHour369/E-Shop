package com.example.eshop.auth.controller;

import com.example.eshop.auth.dto.request.Login;
import com.example.eshop.auth.dto.request.RefreshTokenRequest;
import com.example.eshop.auth.dto.request.VerifyUserDto;
import com.example.eshop.auth.dto.response.AuthenticationResponse;
import com.example.eshop.auth.service.impl.AuthServiceImpl;
import com.example.eshop.auth.service.impl.SessionCookieService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Browser session API. Bearer clients continue to use LoginController. */
@RestController
@RequestMapping(SessionCookieService.SESSION_PATH)
public class SessionController {
    private final AuthServiceImpl auth;
    private final SessionCookieService cookies;

    public SessionController(AuthServiceImpl auth, SessionCookieService cookies) {
        this.auth = auth;
        this.cookies = cookies;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @RequestHeader(value = "Origin", required = false) String origin,
            @Valid @RequestBody Login request) {
        cookies.requireTrustedOrigin(origin);
        return session(auth.authenticate(request));
    }

    @PostMapping("/verify")
    public ResponseEntity<AuthenticationResponse> verify(
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestParam String email, @RequestParam String code) {
        cookies.requireTrustedOrigin(origin);
        VerifyUserDto request = new VerifyUserDto();
        request.setEmail(email);
        request.setVerificationCode(code);
        return session(auth.verifyUser(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refresh(
            @RequestHeader(value = "Origin", required = false) String origin,
            @CookieValue(value = SessionCookieService.REFRESH_COOKIE, required = false) String token) {
        cookies.requireTrustedOrigin(origin);
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh cookie is required");
        }
        return session(auth.refreshToken(refreshRequest(token)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Origin", required = false) String origin,
            @CookieValue(value = SessionCookieService.REFRESH_COOKIE, required = false) String token) {
        cookies.requireTrustedOrigin(origin);
        if (token != null && !token.isBlank()) {
            auth.logout(refreshRequest(token));
        }
        return ResponseEntity.noContent().headers(cookies.clear()).build();
    }

    private ResponseEntity<AuthenticationResponse> session(AuthenticationResponse result) {
        return ResponseEntity.ok().headers(cookies.issue(result)).body(cookies.withoutTokens(result));
    }

    private RefreshTokenRequest refreshRequest(String token) {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(token);
        return request;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<AuthenticationResponse> statusError(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).cacheControl(org.springframework.http.CacheControl.noStore())
                .body(AuthenticationResponse.builder().message(error.getReason()).build());
    }

    @ExceptionHandler(com.example.eshop.common.exception.CustomMessageException.class)
    public ResponseEntity<AuthenticationResponse> authenticationError(
            com.example.eshop.common.exception.CustomMessageException error) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).cacheControl(org.springframework.http.CacheControl.noStore())
                .body(AuthenticationResponse.builder().message("Authentication failed").build());
    }
}
