package com.example.eshop.catalog.controller;
import com.example.eshop.catalog.model.*;
import com.example.eshop.catalog.repository.ProductSkuRepository;
import com.example.eshop.catalog.service.impl.ProductResponseService;
import com.example.eshop.catalog.dto.response.ProductResponse;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class CheckoutProductControllerTest {
    @Test void returnsActualActiveProductForSku() {
        var skus=mock(ProductSkuRepository.class);var responses=mock(ProductResponseService.class);
        var product=new Product();product.setIsActive(true);product.setDeleted(false);
        var sku=new ProductSku();sku.setProduct(product);when(skus.findById(5L)).thenReturn(Optional.of(sku));
        var response=new ProductResponse();response.setName("Actual catalog product");when(responses.toProductResponse(product)).thenReturn(response);
        assertThat(new CheckoutProductController(skus,responses).bySku(5)).isSameAs(response);
    }
    @Test void inactiveDeletedAndMissingProductsAreNotExposed() {
        var skus=mock(ProductSkuRepository.class);var responses=mock(ProductResponseService.class);
        var controller=new CheckoutProductController(skus,responses);
        assertThatThrownBy(()->controller.bySku(5)).hasMessage("Product not found");
        for(boolean deleted:new boolean[]{false,true}) {
            var product=new Product();product.setIsActive(deleted);product.setDeleted(deleted);
            var sku=new ProductSku();sku.setProduct(product);when(skus.findById(5L)).thenReturn(Optional.of(sku));
            assertThatThrownBy(()->controller.bySku(5)).hasMessage("Product not found");
        }
        verifyNoInteractions(responses);
    }
}
