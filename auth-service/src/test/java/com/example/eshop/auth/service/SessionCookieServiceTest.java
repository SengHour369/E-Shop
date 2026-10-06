package com.example.eshop.auth.service;

import com.example.eshop.auth.dto.response.AuthenticationResponse;
import com.example.eshop.auth.service.impl.SessionCookieService;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class SessionCookieServiceTest {
    @Test void configuredFrontendCanUseCrossSiteCookies() {
        var cookies = new SessionCookieService(true, "None", 900, Set.of("https://shop.example.org"));
        cookies.requireTrustedOrigin("https://shop.example.org");
        assertThatThrownBy(() -> cookies.requireTrustedOrigin("https://untrusted.example.org"))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(cookies.issue(new AuthenticationResponse(1L, "access", "refresh"))
                .get(HttpHeaders.SET_COOKIE)).hasSize(2).allSatisfy(value ->
                assertThat(value).contains("SameSite=None", "Secure", "HttpOnly").doesNotContain("Domain="));
        assertThat(cookies.clear().get(HttpHeaders.SET_COOKIE)).hasSize(2).allSatisfy(value ->
                assertThat(value).contains("SameSite=None", "Max-Age=0"));
    }

    @Test void invalidCookieConfigurationFailsAtStartup() {
        assertThatThrownBy(() -> new SessionCookieService(false, "None", 900, Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SessionCookieService(true, "anything", 900, Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
