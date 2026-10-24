package com.example.eshop.catalog.controller;

import com.example.eshop.catalog.repository.*;
import com.example.eshop.catalog.service.impl.ProductResponseService;
import com.example.eshop.common.storefront.StorefrontAccessClient;
import com.example.eshop.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;

@RestController
@RequestMapping("/api/v1/storefront")
@RequiredArgsConstructor
public class StorefrontProductController {

    private final com.example.eshop.catalog.service.CategoryService categories;
    private final ProductRepository products;
    private final ProductSkuRepository skus;
    private final ProductResponseService responses;
    private final StorefrontAccessClient access;

    @GetMapping("/categories")
    public Object categories() {
        access.require(access.current().guestBrowsing());
        return categories.getAllCategories(
                PageRequest.of(0, 100, Sort.by("name"))
        );
    }

    @GetMapping("/categories/name")
    public Object categoryName(@RequestParam String name) {
        access.require(access.current().guestBrowsing());
        return categories.getCategoryByName(name);
    }

    @GetMapping("/categories/{id}")
    public Object category(@PathVariable long id) {
        access.require(access.current().guestBrowsing());
        return categories.getCategoryWithSubCategories(id);
    }

    @GetMapping("/products")
    @Transactional(readOnly = true)
    public Object list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String category
    ) {
        access.require(access.current().guestBrowsing());

        if (page < 1
                || size < 1
                || size > 48
                || search.length() > 120) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST
            );
        }

        var pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by("id").descending()
        );
        var result = category.isBlank()
                ? products.searchByNameAndActive(
                search.trim(),
                true,
                pageable
        )
                : products.storefrontCategory(category, pageable);
        return java.util.Map.of(
                "payload", responses.toProductResponses(result.getContent()),
                "total_items", result.getTotalElements(),
                "total_pages", result.getTotalPages(),
                "current_page", page,
                "page_size", size
        );
    }

    @GetMapping("/products/{id}")
    @Transactional(readOnly = true)
    public Object detail(@PathVariable long id) {
        access.require(access.current().guestBrowsing());
        var product = products.findByIdNotDeleted(id)
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found"
                ));
        return responses.toProductResponse(product);
    }

    @GetMapping("/skus/{id}")
    @Transactional(readOnly = true)
    public Object sku(@PathVariable long id) {
        access.require(access.current().guestBrowsing());
        var sku = skus.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found"
                ));
        var p = sku.getProduct();

        if (!Boolean.TRUE.equals(p.getIsActive())
                || Boolean.TRUE.equals(p.getDeleted())) {
            throw new ResourceNotFoundException("Product not found");
        }

        return responses.toProductResponse(p);
    }
}