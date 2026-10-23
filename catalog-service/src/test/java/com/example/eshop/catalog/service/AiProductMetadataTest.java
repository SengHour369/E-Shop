package com.example.eshop.catalog.service;
import com.example.eshop.catalog.controller.AiCatalogController.ProductCard;
import com.example.eshop.catalog.dto.response.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
class AiProductMetadataTest {
 @Test void derivesOnlyStoredBrandAndKeepsActualSkuPriceStockAndSpecifications(){
  var sku=new ProductSkuResponse();sku.setId(42L);sku.setSku("REAL-42");sku.setPrice(new BigDecimal("100"));sku.setFinalPrice(new BigDecimal("80"));sku.setDescription("Stored specification");
  var inventory=new InventoryResponse();inventory.setAvailableQuantity(3L);sku.setInventory(inventory);
  sku.setProductAttributeResponse(List.of(new ProductAttributeResponse(1L,"Brand",List.of(new ProductAttributeValueResponse(2L,"Stored brand")))));
  var product=new ProductResponse();product.setId(11L);product.setName("Name is not a brand");product.setSkus(List.of(sku));
  var card=ProductCard.from(product,"Stored category");assertThat(card.brand()).isEqualTo("Stored brand");assertThat(card.category()).isEqualTo("Stored category");assertThat(card.productCode()).isNull();assertThat(card.currency()).isEqualTo("USD");assertThat(card.skus().get(0).available()).isEqualTo(3);assertThat(card.skus().get(0).description()).isEqualTo("Stored specification");assertThat(card.skus().get(0).effectivePrice()).isEqualByComparingTo("80");
  assertThat(card.withSkus(card.skus()).brand()).isEqualTo(card.brand());sku.setProductAttributeResponse(List.of());assertThat(ProductCard.from(product).brand()).isNull();
 }
}
