package com.example.eshop.catalog.service;

import com.example.eshop.catalog.dto.request.GetProductRequest;
import com.example.eshop.catalog.repository.ProductRepository;
import com.example.eshop.catalog.repository.SubCategoryRepository;
import com.example.eshop.catalog.service.impl.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;

class ProductFilterTest {
    @Test void omittedCriteriaListsProductsWithoutNullUnboxing() {
        ProductRepository repository = mock(ProductRepository.class);
        ProductResponseService responses = mock(ProductResponseService.class);
        when(repository.findAllNotDeleted(any(Pageable.class))).thenReturn(Page.empty());
        when(responses.toProductResponses(List.of())).thenReturn(List.of());
        service(repository, responses).getProducts(new GetProductRequest());
        verify(repository).findAllNotDeleted(any(Pageable.class));
    }

    @Test void activeFilterDoesNotRequireCriteriaValue() {
        ProductRepository repository = mock(ProductRepository.class);
        ProductResponseService responses = mock(ProductResponseService.class);
        when(repository.findByIsActiveTrue(any(Pageable.class))).thenReturn(Page.empty());
        when(responses.toProductResponses(List.of())).thenReturn(List.of());
        GetProductRequest request = new GetProductRequest();
        request.setCriteriaType(4);
        service(repository, responses).getProducts(request);
        verify(repository).findByIsActiveTrue(any(Pageable.class));
        verify(repository, never()).findAllNotDeleted(any(Pageable.class));
    }

    @Test void pageSizeIsBounded() {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            GetProductRequest request = new GetProductRequest();
            request.setSize(101);
            assertThat(factory.getValidator().validate(request)).isNotEmpty();
            request.setSize(10);
            request.setPage(0);
            assertThat(factory.getValidator().validate(request)).isNotEmpty();
            request.setPage(1);
            assertThat(factory.getValidator().validate(request)).isEmpty();
        }
    }

    private ProductServiceImpl service(ProductRepository products, ProductResponseService responses) {
        return new ProductServiceImpl(mock(com.example.eshop.common.audit.AuditLogService.class),
                products, mock(SubCategoryRepository.class), mock(ImageService.class),
                responses, mock(ProductSkuServiceImpl.class));
    }
}
