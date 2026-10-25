package com.example.eshop.payment.automation;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name="automation.enabled",havingValue="true")
public class BakongAppPaymentController {
    private final BakongAppPayment appPayment;
    @PostMapping("/api/v1/payments/orders/{id}/bakong-app")
    public ResponseEntity<Map<String,Object>> launch(@PathVariable long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(appPayment.launch(id));
    }
}
