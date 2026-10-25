package com.example.eshop.catalog.dto;

import com.example.eshop.catalog.dto.response.StockMovementResponse;
import com.example.eshop.catalog.dto.response.ResponseErrorTemplate;
import com.example.eshop.catalog.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class StockMovementResponseTest {
    @Test void historySerializesAsBoundedMovementDataWithoutTraversingTheProductGraph() throws Exception {
        var product = new Product();
        var sku = new ProductSku();
        sku.setProduct(product);
        product.setProductSkus(List.of(sku));
        var inventory = new Inventory();
        inventory.setId(52L);
        inventory.setProductSku(sku);
        var movement = StockMovement.builder().id(7L).inventory(inventory)
                .quantityChange(-2L).previousQuantity(10L).newQuantity(8L)
                .movementType("ORDER_PLACED").performedBy("admin").build();
        var mapper = new ObjectMapper().findAndRegisterModules();
        var response = ResponseErrorTemplate.success("History retrieved", new PageImpl<>(List.of(movement),org.springframework.data.domain.PageRequest.of(0,10),1).map(StockMovementResponse::from));
        var json = mapper.writeValueAsString(response);
        var row = mapper.readTree(json).path("data").path("content").get(0);
        assertThat(row.path("inventoryId").asLong()).isEqualTo(52L);
        assertThat(row.path("quantityChange").asLong()).isEqualTo(-2L);
        assertThat(row.path("newQuantity").asLong()).isEqualTo(8L);
        assertThat(row.has("inventory")).isFalse();
        assertThat(json).hasSizeLessThan(2000);
    }
}
