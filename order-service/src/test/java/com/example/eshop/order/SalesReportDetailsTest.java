package com.example.eshop.order;
import com.example.eshop.order.repository.OrderItemRepository;
import com.example.eshop.order.client.CatalogCheckoutClient;
import com.example.eshop.order.service.SalesReportDetailsService;
import com.example.eshop.order.dto.response.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import java.util.*;
import java.math.BigDecimal;
class SalesReportDetailsTest {
 @Test void groupsRecordedItemAmountsAndNeverInventsUnavailableCategories(){
  var items=mock(OrderItemRepository.class);var catalog=mock(CatalogCheckoutClient.class);
  when(items.monthlyItems(any(),any())).thenReturn(List.of(new MonthlyItemsResponse(3,5L)));
  when(items.salesBySku(any(),any())).thenReturn(List.of(new SkuSalesResponse(1L,2L,new BigDecimal("20.00")),new SkuSalesResponse(2L,3L,new BigDecimal("30.00"))));
  when(catalog.salesCategories(eq("Bearer test"),anyList())).thenReturn(Map.of("1","Shoes","2","Shoes"));
  var service=new SalesReportDetailsService(items,catalog);var result=service.yearly(2026,"Bearer test");assertThat(result.categories()).hasSize(1);assertThat(result.categories().get(0).amount()).isEqualByComparingTo("50.00");assertThat(result.categories().get(0).items()).isEqualTo(5);
  when(catalog.salesCategories(anyString(),anyList())).thenThrow(new IllegalStateException());var unavailable=service.yearly(2026,"Bearer test");assertThat(unavailable.categoriesAvailable()).isFalse();assertThat(unavailable.categories()).isEmpty();assertThat(unavailable.monthlyItems()).hasSize(1);
 }
}
