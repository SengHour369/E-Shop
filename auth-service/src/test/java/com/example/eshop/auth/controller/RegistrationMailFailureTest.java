package com.example.eshop.auth.controller;

import com.example.eshop.auth.exception.GlobalExceptionHandler;
import com.example.eshop.auth.jwt.JwtService;
import com.example.eshop.auth.repository.UserRepository;
import com.example.eshop.auth.security.UserDetailsService;
import com.example.eshop.auth.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RegistrationMailFailureTest {
    @Test
    void smtpAuthenticationFailureReturnsServiceUnavailableWithoutProviderDetails() throws Exception {
        var auth = mock(AuthServiceImpl.class);
        when(auth.create(any())).thenThrow(new MailAuthenticationException("private SMTP details"));
        var mvc = MockMvcBuilders.standaloneSetup(new LoginController(
                mock(JwtService.class), mock(UserDetailsService.class), auth, mock(UserRepository.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(post("/api/v1/public/register").contentType("application/json")
                        .content("""
                                {"username":"mail-test","password":"Test-password-123!",
                                 "email":"mail-test@example.test","full_name":"Mail Test"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_DELIVERY_FAILED"))
                .andExpect(jsonPath("$.debug_message").doesNotExist())
                .andExpect(jsonPath("$.message").value(
                        "Unable to send email. Please try again later or contact support."));
    }
}
