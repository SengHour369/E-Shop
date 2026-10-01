package com.example.eshop.catalog.controller;
import com.example.eshop.catalog.dto.response.ResponseErrorTemplate;
import com.example.eshop.catalog.service.impl.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/promotions") @RequiredArgsConstructor
public class PromotionController {
    private final PromotionService service;
    @GetMapping("/active")
    public ResponseErrorTemplate active(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseErrorTemplate.success("Active promotions", service.active(page, size));
    }
    @GetMapping("/{id}/products")
    public ResponseErrorTemplate products(@PathVariable Long id, @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size) {
        return ResponseErrorTemplate.success("Promotion products", service.products(id, page, size));
    }
}

