package com.example.eshop.catalog.controller;
import com.example.eshop.catalog.dto.request.*;
import com.example.eshop.catalog.dto.response.ResponseErrorTemplate;
import com.example.eshop.catalog.enumeration.PromotionStatus;
import com.example.eshop.catalog.service.impl.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin/promotions")
@RequiredArgsConstructor @PreAuthorize("hasAuthority('ADMIN')")
public class PromotionAdminController {
    private final PromotionService service;
    @PostMapping
    public ResponseErrorTemplate create(@Valid @RequestBody PromotionRequest request) {
        return ResponseErrorTemplate.success("Promotion created", service.create(request));
    }
    @PutMapping("/{id}")
    public ResponseErrorTemplate update(@PathVariable Long id, @Valid @RequestBody PromotionRequest request) {
        return ResponseErrorTemplate.success("Promotion updated", service.update(id, request));
    }
    @GetMapping("/{id}")
    public ResponseErrorTemplate get(@PathVariable Long id) {
        return ResponseErrorTemplate.success("Promotion", service.get(id));
    }
    @GetMapping
    public ResponseErrorTemplate list(@RequestParam(required = false) PromotionStatus status,
        @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseErrorTemplate.success("Promotions", service.list(status, page, size));
    }
    @DeleteMapping("/{id}")
    public ResponseErrorTemplate archive(@PathVariable Long id) {
        return ResponseErrorTemplate.success("Promotion disabled", service.transition(id, "disable"));
    }
    @PostMapping("/{id}/skus")
    public ResponseErrorTemplate assign(@PathVariable Long id, @Valid @RequestBody PromotionSkuRequest request) {
        service.assign(id, request); return ResponseErrorTemplate.success("SKUs assigned", null);
    }
    @GetMapping("/{id}/skus")
    public ResponseErrorTemplate assigned(@PathVariable Long id, @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size) {
        return ResponseErrorTemplate.success("Assigned SKUs", service.assigned(id, page, size));
    }
    @DeleteMapping("/{id}/skus/{skuId}")
    public ResponseErrorTemplate remove(@PathVariable Long id, @PathVariable Long skuId) {
        service.remove(id, skuId); return ResponseErrorTemplate.success("SKU removed", null);
    }
    @PostMapping("/{id}/schedule")
    public ResponseErrorTemplate schedule(@PathVariable Long id) {
        return ResponseErrorTemplate.success("Promotion scheduled", service.transition(id, "schedule"));
    }
    @PostMapping("/{id}/activate")
    public ResponseErrorTemplate activate(@PathVariable Long id) {
        return ResponseErrorTemplate.success("Promotion activated", service.transition(id, "activate"));
    }
    @PostMapping("/{id}/disable")
    public ResponseErrorTemplate disable(@PathVariable Long id) {
        return ResponseErrorTemplate.success("Promotion disabled", service.transition(id, "disable"));
    }
}

