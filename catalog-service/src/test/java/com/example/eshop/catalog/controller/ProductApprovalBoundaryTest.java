package com.example.eshop.catalog.controller;
import com.example.eshop.catalog.service.ProductService;
import com.example.eshop.catalog.dto.request.ProductRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
class ProductApprovalBoundaryTest {
 @Test void everyLegacyProductMutationRejectsBeforeCallingBusinessServices() {
  var service=mock(ProductService.class);var controller=new ProductController(service,new ObjectMapper());
  assertThatThrownBy(()->controller.createProduct("Proposed","Description",true,1L,null,List.of(),null)).hasMessageContaining("APPROVAL_REQUIRED");
  assertThatThrownBy(()->controller.updateProduct(1L,"Proposed","Description",true,1L,null,List.of(),null)).hasMessageContaining("APPROVAL_REQUIRED");
  assertThatThrownBy(()->controller.updateProductJson(1L,new ProductRequest())).hasMessageContaining("APPROVAL_REQUIRED");
  assertThatThrownBy(()->controller.updateProductStatus(1L,false)).hasMessageContaining("APPROVAL_REQUIRED");
  assertThatThrownBy(()->controller.deleteProduct(1L)).hasMessageContaining("APPROVAL_REQUIRED");
  verifyNoInteractions(service);
 }
 @Test void legacyAttributeMutationCannotBypassAProductApproval() {
  var attributes=mock(com.example.eshop.catalog.service.ProductAttributeService.class);
  var values=mock(com.example.eshop.catalog.service.ProductAttributeValueService.class);
  assertThatThrownBy(()->new ProductAttributeController(attributes).deleteAttribute(1L)).hasMessageContaining("APPROVAL_REQUIRED");
  var controller=new ProductAttributeValueController(values);
  assertThatThrownBy(()->controller.updateAttributeValue(1L,new com.example.eshop.catalog.dto.request.ProductAttributeValueRequest())).hasMessageContaining("APPROVAL_REQUIRED");
  assertThatThrownBy(()->controller.deleteAttributeValue(1L)).hasMessageContaining("APPROVAL_REQUIRED");verifyNoInteractions(attributes,values);
 }

}
