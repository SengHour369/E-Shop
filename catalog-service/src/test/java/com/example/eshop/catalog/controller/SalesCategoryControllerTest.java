package com.example.eshop.catalog.controller;
import com.example.eshop.catalog.repository.ProductSkuRepository;
import com.example.eshop.catalog.service.impl.ProductResponseService;
import com.example.eshop.common.security.LivePermissionService;
import com.example.eshop.catalog.model.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class SalesCategoryControllerTest {
 @Test void requiresReportPermissionBeforeReadingCategories(){
  var skus=mock(ProductSkuRepository.class);var responses=mock(ProductResponseService.class);var permissions=mock(LivePermissionService.class);
  doThrow(new org.springframework.security.access.AccessDeniedException("Report access required")).when(permissions).require("REPORT_VIEW");
  assertThatThrownBy(()->new SalesCategoryController(skus,responses,permissions).categories(List.of(1L))).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);verifyNoInteractions(skus,responses);
 }
 @Test void resolvesExistingSkuCategoriesAndLabelsMissingCategoriesHonestly(){
  var skus=mock(ProductSkuRepository.class);var responses=mock(ProductResponseService.class);var permissions=mock(LivePermissionService.class);
  var product=new Product();product.setId(5L);var sku=new ProductSku();sku.setId(1L);sku.setProduct(product);
  when(skus.findAllById(List.of(1L))).thenReturn(List.of(sku));when(responses.aiCategoryNames(List.of(5L))).thenReturn(Map.of(5L,"Shoes"));
  var controller=new SalesCategoryController(skus,responses,permissions);assertThat(controller.categories(List.of(1L))).containsEntry("1","Shoes");verify(permissions).require("REPORT_VIEW");
 }
}
