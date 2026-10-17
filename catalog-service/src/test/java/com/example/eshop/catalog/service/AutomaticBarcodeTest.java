package com.example.eshop.catalog.service;
import com.example.eshop.catalog.model.ProductSku;
import com.example.eshop.catalog.repository.ProductSkuRepository;
import com.example.eshop.catalog.service.impl.ProductSkuServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class AutomaticBarcodeTest {
 @Mock ProductSkuRepository repository;
 @InjectMocks ProductSkuServiceImpl service;
 @Test void generatesUniqueInternalBarcodesWhenOmitted(){
  var first=new ProductSku();var second=new ProductSku();
  ReflectionTestUtils.invokeMethod(service,"assignBarcode",first,null,null);
  ReflectionTestUtils.invokeMethod(service,"assignBarcode",second," ",null);
  assertThat(first.getBarcode()).matches("ESHOP-[A-F0-9]{32}");
  assertThat(second.getBarcode()).isNotEqualTo(first.getBarcode());
  verify(repository).existsByBarcode(first.getBarcode());
 }
 @Test void preservesExistingBarcodeWhenAnEditLeavesItBlank(){
  var sku=new ProductSku();sku.setBarcode("EXISTING-123");
  ReflectionTestUtils.invokeMethod(service,"assignBarcode",sku,"",7L);
  assertThat(sku.getBarcode()).isEqualTo("EXISTING-123");verifyNoInteractions(repository);
 }
 @Test void stillAcceptsExplicitBarcode(){
  var sku=new ProductSku();ReflectionTestUtils.invokeMethod(service,"assignBarcode",sku,"custom-123",null);
  assertThat(sku.getBarcode()).isEqualTo("CUSTOM-123");
 }
}
