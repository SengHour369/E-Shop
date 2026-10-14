package com.example.eshop.order;
import com.example.eshop.order.model.OrderDetail;
import com.example.eshop.order.repository.OrderRepository;
import com.example.eshop.order.repository.ReturnRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
@DataJpaTest(properties={"spring.jpa.hibernate.ddl-auto=create-drop","spring.cloud.discovery.enabled=false"})
@org.springframework.test.context.ContextConfiguration(classes=MonthlyReportRepositoryTest.Config.class)
class CustomerReturnRepositoryTest {
 @Autowired OrderRepository orders;
 @Autowired ReturnRequestRepository returns;
 @Test void listRequiresBothOrderAndReturnOwnership(){
  save("OWNED",7L,7L);save("FOREIGN_ORDER",7L,8L);save("FOREIGN_RETURN",8L,7L);save("OTHER",8L,8L);
  var result=returns.findOwnedByCustomer(7L,PageRequest.of(0,10));
  assertThat(result.getTotalElements()).isEqualTo(1);assertThat(result.getContent().get(0).getReturnId()).isEqualTo("OWNED");
 }
 private void save(String id,Long customerId,Long orderCustomerId){
  var order=new OrderDetail();order.setUserId(orderCustomerId);order.setOrderNumber("ORD-"+id);order.setStatus("DELIVERED");order.setOrderDate(LocalDateTime.now());order.setTotalAmount(BigDecimal.TEN);orders.saveAndFlush(order);
  var r=new com.example.eshop.order.model.Return();r.setReturnId(id);r.setOrderId(order.getId());r.setCustomerId(customerId);r.setProductId(1L);r.setReturnType("REFUND");r.setStatus("REQUESTED");r.setAmount(BigDecimal.TEN);r.setRequestedAt(LocalDateTime.now());returns.saveAndFlush(r);
 }
}
