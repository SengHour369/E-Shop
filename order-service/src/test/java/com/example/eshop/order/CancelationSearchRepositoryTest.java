package com.example.eshop.order;
import com.example.eshop.order.model.OrderCancelation;
import com.example.eshop.order.repository.OrderCancelationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
@DataJpaTest(properties={"spring.jpa.hibernate.ddl-auto=create-drop","spring.cloud.discovery.enabled=false"})
@org.springframework.test.context.ContextConfiguration(classes=MonthlyReportRepositoryTest.Config.class)
class CancelationSearchRepositoryTest {
 @Autowired OrderCancelationRepository repository;
 private void save(String number,String name,String status,BigDecimal amount) {
  repository.saveAndFlush(OrderCancelation.builder().orderId(1L).orderNo(number).customerId(1L).customerName(name)
   .cancelReason("CUSTOMER_REQUEST").cancelStatus(status).cancelDate(LocalDateTime.of(2026,10,18,10,0)).amount(amount).build());
 }
 @Test void absentFiltersListAndPaginateWithoutUntypedNulls() {
  save("ORD-1","Alice","REQUESTED",BigDecimal.TEN);save("ORD-2","Bob","APPROVED",BigDecimal.ONE);
  var result=repository.search(null,null,null,null,null,null,null,null,PageRequest.of(0,1));
  assertThat(result.getTotalElements()).isEqualTo(2);assertThat(result.getContent()).hasSize(1);
 }
 @Test void combinesCaseInsensitiveNameStatusDateAndAmountFilters() {
  save("ORD-1","Alice Smith","REQUESTED",BigDecimal.TEN);save("ORD-2","Alice Brown","APPROVED",BigDecimal.ONE);
  var result=repository.search(null,"ALICE","CUSTOMER_REQUEST","REQUESTED",LocalDateTime.of(2026,10,18,0,0),LocalDateTime.of(2026,10,19,0,0),BigDecimal.valueOf(5),BigDecimal.valueOf(20),PageRequest.of(0,10));
  assertThat(result.getContent()).extracting("orderNo").containsExactly("ORD-1");
  assertThat(repository.search("MISSING",null,null,null,null,null,null,null,PageRequest.of(0,10))).isEmpty();
 }
}
