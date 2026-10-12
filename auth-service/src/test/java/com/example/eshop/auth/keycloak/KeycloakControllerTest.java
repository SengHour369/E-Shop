package com.example.eshop.auth.keycloak;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class KeycloakControllerTest {
    @Test void rejectedExchangePreservesUnauthorizedAndDoesNotCache() throws Exception {
        var service = mock(KeycloakExchangeService.class);
        when(service.exchange("forged")).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        var mvc = MockMvcBuilders.standaloneSetup(new KeycloakController(service, true, "http://localhost:8180", "eshop", "eshop-web"))
                .setControllerAdvice(new com.example.eshop.auth.exception.GlobalExceptionHandler()).build();
        mvc.perform(post("/api/v1/public/keycloak/exchange").contentType(MediaType.APPLICATION_JSON).content("{\"accessToken\":\"forged\"}"))
                .andExpect(status().isUnauthorized()).andExpect(header().string("Cache-Control", "no-store"));
    }
}
