package com.example.eshop.payment.automation;
import com.example.eshop.common.automation.VerifiedPaymentSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequiredArgsConstructor
@ConditionalOnProperty(name="automation.enabled",havingValue="true")
public class PaymentAutomationController {
    private final PaymentAutomation automation;
    private final com.example.eshop.payment.bakong.service.BakongService bakong;
    @GetMapping(value="/api/v1/payments/orders/{id}/qr/image",produces="image/png")
    public byte[] image(@PathVariable long id) {
        var qr=automation.customerQr(id);
        if(!"PENDING".equals(qr.get("status").toString()) || java.time.Instant.now().isAfter((java.time.Instant)qr.get("expiresAt")))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"QR is no longer payable");
        return bakong.getQRImage(new com.example.eshop.payment.bakong.dto.GetQRImageRequest(qr.get("qr").toString(),""));
    }
    @GetMapping("/api/v1/payments/orders/{id}/qr") public Map<String,Object> qr(@PathVariable long id) { return automation.customerQr(id); }
    @PostMapping("/api/v1/payments/orders/{id}/verify") public Map<String,Object> verify(@PathVariable long id) {
        var payment=automation.customerQr(id); automation.verify(((Number)payment.get("paymentId")).longValue()); return automation.customerQr(id);
    }
    @GetMapping("/internal/commerce/payments/{id}") @PreAuthorize("hasAuthority('SERVICE_ORDER')")
    public VerifiedPaymentSnapshot snapshot(@PathVariable long id) {return automation.snapshot(id);}
}
