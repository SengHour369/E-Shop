package com.example.eshop.catalog.controller;

import com.example.eshop.catalog.repository.ProductSkuRepository;
import com.example.eshop.catalog.service.impl.ProductResponseService;
import com.example.eshop.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Public active catalog presentation for cart/checkout, never a source of payable totals. */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class CheckoutProductController {
    private final ProductSkuRepository skus;
    private final ProductResponseService responses;
    @GetMapping("/sku/{id}")
    @Transactional(readOnly=true)
    public Object bySku(@PathVariable long id) {
        var sku=skus.findById(id).orElseThrow(()->new ResourceNotFoundException("Product not found"));
        var product=sku.getProduct();
        if(product==null||!Boolean.TRUE.equals(product.getIsActive())||Boolean.TRUE.equals(product.getDeleted()))
            throw new ResourceNotFoundException("Product not found");
        return responses.toProductResponse(product);
    }
}
