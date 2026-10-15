package com.example.eshop.auth.keycloak;

import com.example.eshop.auth.dto.response.AuthenticationResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/keycloak")
public class KeycloakController {

    private final KeycloakExchangeService service;
    private final BrowserConfig config;

    public KeycloakController(
            KeycloakExchangeService service,
            @Value("${eshop.keycloak.enabled:false}") boolean enabled,
            @Value("${eshop.keycloak.url:http://localhost:8180}") String url,
            @Value("${eshop.keycloak.realm:eshop}") String realm,
            @Value("${eshop.keycloak.client-id:eshop-web}") String clientId
    ) {
        this.service = service;
        this.config = new BrowserConfig(enabled, url, realm, clientId);
    }

    public record BrowserConfig(
            boolean enabled,
            String url,
            String realm,
            String clientId
    ) {}

    public record ExchangeRequest(
            @NotBlank @Size(max = 16384) String accessToken
    ) {}

    @GetMapping("/config")
    public ResponseEntity<BrowserConfig> config() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(config);
    }

    @PostMapping("/exchange")
    public ResponseEntity<AuthenticationResponse> exchange(
            @Valid @RequestBody ExchangeRequest request
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.exchange(request.accessToken()));
    }

    // Preserve intentional status codes ahead of the application's generic exception advice.
    @ExceptionHandler(
            org.springframework.web.server.ResponseStatusException.class
    )
    public ResponseEntity<java.util.Map<String, String>> rejected(
            org.springframework.web.server.ResponseStatusException ex
    ) {
        return ResponseEntity.status(ex.getStatusCode())
                .cacheControl(CacheControl.noStore())
                .body(java.util.Map.of(
                        "message",
                        "Keycloak sign-in is unavailable or was rejected."
                ));
    }
}