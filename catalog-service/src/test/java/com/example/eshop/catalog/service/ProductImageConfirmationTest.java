package com.example.eshop.catalog.service;
import com.example.eshop.catalog.service.impl.*;
import com.example.eshop.catalog.repository.*;
import com.example.eshop.catalog.model.*;
import com.example.eshop.catalog.dto.request.*;
import com.example.eshop.common.audit.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class ProductImageConfirmationTest {
 @Test void imageProviderFailureCannotCreateAnImagelessProduct()throws Exception{
  var products=mock(ProductRepository.class);var subcategories=mock(SubCategoryRepository.class);var images=mock(ImageService.class);var responses=mock(ProductResponseService.class);var skus=mock(ProductSkuServiceImpl.class);var audit=mock(AuditLogService.class);
  var service=new ProductServiceImpl(audit,products,subcategories,images,responses,skus);when(subcategories.findById(1L)).thenReturn(Optional.of(new SubCategory()));
  assertThatThrownBy(()->service.createProduct(ProductRequest.builder().name("Phone").subCategoryId(1L).build(),List.of(new MockMultipartFile("file","image.png","image/png",new byte[]{1})),List.of())).hasMessageContaining("did not confirm every image");verify(products,never()).save(any());verifyNoInteractions(responses);
 }
}
