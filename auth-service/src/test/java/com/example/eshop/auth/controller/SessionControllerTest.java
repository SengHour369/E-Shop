package com.example.eshop.auth.controller;

import com.example.eshop.auth.dto.response.AuthenticationResponse;
import com.example.eshop.auth.service.impl.AuthServiceImpl;
import com.example.eshop.auth.service.impl.SessionCookieService;
import jakarta.servlet.http.Cookie;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

class SessionControllerTest {
    private AuthServiceImpl auth;
    private MockMvc mvc;
    private static final String BASE = "/api/v1/public/session";
    private static final String ORIGIN = "http://localhost:5173";
    private final AuthenticationResponse tokens = new AuthenticationResponse(1L, "access", "refresh");

    @BeforeEach void setup() {
        auth = mock(AuthServiceImpl.class);
        mvc = MockMvcBuilders.standaloneSetup(new SessionController(auth,
                new SessionCookieService(true, 900, Set.of(ORIGIN)))).build();
    }

    @Test void loginSetsScopedHttpOnlyCookiesWithoutTokensInBody() throws Exception {
        when(auth.authenticate(any())).thenReturn(tokens);
        var result = mvc.perform(post(BASE + "/login").header("Origin", ORIGIN)
                .contentType("application/json").content("{\"CriteriaValue\":\"shopper\",\"Password\":\"secret\"}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.access_token").doesNotExist())
                .andExpect(jsonPath("$.refresh_token").doesNotExist()).andReturn();
        assertThat(result.getResponse().getHeaders("Set-Cookie")).hasSize(2)
                .allSatisfy(cookie -> assertThat(cookie).contains("HttpOnly", "Secure", "SameSite=Lax"));
        assertThat(result.getResponse().getHeaders("Set-Cookie").get(1))
                .contains("Path=/api/v1/public/session", "Max-Age=604800");
    }

    @Test void refreshUsesCookieAndRotatesBothCookies() throws Exception {
        when(auth.refreshToken(any())).thenReturn(tokens);
        mvc.perform(post(BASE + "/refresh").header("Origin", ORIGIN).cookie(new Cookie("eshop_refresh", "old")))
                .andExpect(status().isOk()).andExpect(header().exists("Set-Cookie"));
        verify(auth).refreshToken(argThat(request -> "old".equals(request.getRefreshToken())));
    }

    @Test void logoutRevokesRefreshAndClearsMatchingCookiePaths() throws Exception {
        var result = mvc.perform(post(BASE + "/logout").header("Origin", ORIGIN)
                .cookie(new Cookie("eshop_refresh", "old"))).andExpect(status().isNoContent()).andReturn();
        verify(auth).logout(argThat(request -> "old".equals(request.getRefreshToken())));
        assertThat(result.getResponse().getHeaders("Set-Cookie")).hasSize(2)
                .allSatisfy(cookie -> assertThat(cookie).contains("Max-Age=0"));
    }

    @Test void missingRefreshIsUnauthorizedAndMissingOriginIsForbidden() throws Exception {
        mvc.perform(post(BASE + "/refresh").header("Origin", ORIGIN)).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE + "/logout")).andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/logout").header("Origin", "https://attacker.example"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(auth);
    }
}
