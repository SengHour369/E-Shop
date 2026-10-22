package com.example.eshop.auth.controller;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
import com.example.eshop.auth.jwt.JwtService;
import com.example.eshop.auth.security.UserDetailsService;
import com.example.eshop.auth.service.impl.AuthServiceImpl;
import com.example.eshop.auth.repository.UserRepository;
class ResetPasswordPageTest {
    MockMvc mvc=MockMvcBuilders.standaloneSetup(new LoginController(mock(JwtService.class),mock(UserDetailsService.class),mock(AuthServiceImpl.class),mock(UserRepository.class))).build();
    @Test void missingTokenShowsRecoveryHtmlInsteadOfJsonError() throws Exception {
        mvc.perform(get("/api/v1/public/reset-password-page")).andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/html"))
            .andExpect(content().string(containsString("Send a new reset link")))
            .andExpect(header().string("Cache-Control","no-store"));
    }
    @Test void completeLinkShowsPasswordFormWithoutReflectingToken() throws Exception {
        mvc.perform(get("/api/v1/public/reset-password-page").param("token","private-test-token"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Reset your password")))
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("private-test-token"))));
    }
}
