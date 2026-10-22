package com.example.eshop.payment.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import com.example.eshop.common.dto.APIResponse;

/** Keep disabled payment routes explicit rather than reporting an internal server error. */
@RestController
@ConditionalOnProperty(name="automation.enabled", havingValue="false", matchIfMissing=true)
public class UnavailableKhqrController {
    @GetMapping({"/api/v1/payments/orders/{id}/qr", "/api/v1/payments/orders/{id}/qr/image"})
    public ResponseEntity<APIResponse<Object>> qr(@PathVariable long id) {
        return unavailable();
    }
    @PostMapping("/api/v1/payments/orders/{id}/verify")
    public ResponseEntity<APIResponse<Object>> verify(@PathVariable long id) {
        return unavailable();
    }
    private ResponseEntity<APIResponse<Object>> unavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(APIResponse.error(
                "KHQR is not configured for this store. Contact the store to arrange payment.", 503));
    }
}
