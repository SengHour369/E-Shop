package com.example.eshop.catalog.scanner;
import com.example.eshop.catalog.repository.*;
import com.example.eshop.catalog.model.Product;
import com.example.eshop.catalog.dto.response.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;
class CatalogPhotoSearchTest {
 @Test void findsImagesAcrossPagesWithoutUsingProductNamesAndFiltersWeakMatches(){
  var products=mock(ProductRepository.class);var skus=mock(ProductSkuRepository.class);var compare=mock(CatalogVisualComparison.class);
  Product one=new Product();one.setId(1L);one.setName("qwerty");Product two=new Product();two.setId(2L);two.setName("different name");
  when(products.findByIsActiveTrue(any())).thenReturn(new PageImpl<>(List.of(one),PageRequest.of(0,50),51),new PageImpl<>(List.of(two),PageRequest.of(1,50),51));
  when(skus.findForScannerProducts(any())).thenReturn(List.of());
  when(products.findImageUrls(List.of(1L))).thenReturn(List.of(image(1L,"image-one")));
  when(products.findImageUrls(List.of(2L))).thenReturn(List.of(image(2L,"image-two")));
  when(compare.compare(any(byte[].class),anyString(),any())).thenAnswer(call->{var input=(ImageMatchResponse)call.getArgument(2);var c=input.getCandidates().get(0);return ImageMatchResponse.builder().matchType("AI_VISUAL_COMPARISON").candidates(List.of(new ImageCandidate(c.getProductId(),null,c.getName(),null,c.getImageUrl(),c.getProductId()==1?.93:.2))).build();});
  var result=new CatalogPhotoSearch(products,skus,compare).search(new byte[]{1},"image/png","request");
  assertThat(result.getCandidates()).extracting(ImageCandidate::getProductId).containsExactly(1L);
  assertThat(result.isExact()).isFalse();assertThat(result.isRequiresSelection()).isTrue();
  verify(products,times(2)).findByIsActiveTrue(any());verify(products,never()).findForScannerName(any(),any());
 }
 @Test void comparisonFailureDoesNotBecomeAFalseMatch(){
  var products=mock(ProductRepository.class);var skus=mock(ProductSkuRepository.class);var compare=mock(CatalogVisualComparison.class);
  var product=new Product();product.setId(1L);product.setName("qwerty");
  when(products.findByIsActiveTrue(any())).thenReturn(new PageImpl<>(List.of(product)));
  when(skus.findForScannerProducts(any())).thenReturn(List.of());when(products.findImageUrls(any())).thenReturn(List.of(image(1L,"image")));
  when(compare.compare(any(byte[].class),anyString(),any())).thenReturn(ImageMatchResponse.builder().matchType("AI_LABEL_CANDIDATES").candidates(List.of()).build());
  assertThat(new CatalogPhotoSearch(products,skus,compare).search(new byte[]{1},"image/png","request").getMatchType()).isEqualTo("AI_UNAVAILABLE");
 }
 private ProductRepository.ProductImage image(long id,String url){return new ProductRepository.ProductImage(){public Long getProductId(){return id;}public String getUrl(){return url;}};}
}
