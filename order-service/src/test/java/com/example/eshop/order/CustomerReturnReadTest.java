package com.example.eshop.order;
import com.example.eshop.order.controller.CustomerReturnController;
import com.example.eshop.order.model.*;
import com.example.eshop.order.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class CustomerReturnReadTest {
    ReturnRequestRepository returns;OrderRepository orders;RefundRepository refunds;ReturnStatusHistoryRepository history;CustomerReturnController controller;
    @BeforeEach void setup(){
        returns=mock(ReturnRequestRepository.class);orders=mock(OrderRepository.class);refunds=mock(RefundRepository.class);history=mock(ReturnStatusHistoryRepository.class);
        controller=new CustomerReturnController(returns,orders,refunds,history);
        var auth=new UsernamePasswordAuthenticationToken("customer",null,List.of());auth.setDetails(Map.of("userId",7L));SecurityContextHolder.getContext().setAuthentication(auth);
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    void owned(){
        var request=new com.example.eshop.order.model.Return();request.setId(1L);request.setReturnId("RET-1");request.setOrderId(2L);request.setCustomerId(7L);request.setProductId(3L);request.setStatus("REQUESTED");request.setReason("No sound");request.setAmount(BigDecimal.TEN);request.setRequestedAt(LocalDateTime.now());
        when(returns.findByReturnId("RET-1")).thenReturn(Optional.of(request));
        var order=new OrderDetail();order.setId(2L);order.setUserId(7L);order.setOrderNumber("ORD-2");when(orders.findById(2L)).thenReturn(Optional.of(order));
        when(history.findByReturnRequestOrderByChangedAtDesc(1L)).thenReturn(List.of());when(refunds.findByReturnId("RET-1")).thenReturn(Optional.empty());
    }
    @Test void returnsOnlyOwnedBusinessData(){owned();var response=(CustomerReturnController.Detail)controller.detail("RET-1",7L).object();assertThat(response.order_no()).isEqualTo("ORD-2");assertThat(response.reason()).isEqualTo("No sound");assertThat(response.refund()).isNull();assertThat(response.history()).isEmpty();}
    @Test void rejectsForgedCustomerBeforeLookingUpReturn(){assertThatThrownBy(()->controller.detail("RET-1",8L)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);verifyNoInteractions(returns,orders,refunds,history);}
    @Test void rejectsSomeoneElsesReturnBeforeLookingUpRelatedRecords(){owned();returns.findByReturnId("RET-1").orElseThrow().setCustomerId(8L);assertThatThrownBy(()->controller.detail("RET-1",7L)).hasMessage("Return not found");verifyNoInteractions(orders,refunds,history);}
    @Test void rejectsReturnLinkedToSomeoneElsesOrder(){owned();orders.findById(2L).orElseThrow().setUserId(8L);assertThatThrownBy(()->controller.detail("RET-1",7L)).hasMessage("Return not found");verifyNoInteractions(refunds,history);}
    @Test void omitsRefundLinkedToAnotherCustomerOrOrder(){owned();var refund=new Refund();refund.setCustomerId(8L);refund.setOrderId(2L);when(refunds.findByReturnId("RET-1")).thenReturn(Optional.of(refund));assertThat(((CustomerReturnController.Detail)controller.detail("RET-1",7L).object()).refund()).isNull();refund.setCustomerId(7L);refund.setOrderId(99L);assertThat(((CustomerReturnController.Detail)controller.detail("RET-1",7L).object()).refund()).isNull();}
    @Test void exposesRecordedRefundStatusWithoutChangingIt(){owned();var refund=new Refund();refund.setCustomerId(7L);refund.setOrderId(2L);refund.setStatus("PROCESSED");refund.setAmount(BigDecimal.TEN);when(refunds.findByReturnId("RET-1")).thenReturn(Optional.of(refund));var result=(CustomerReturnController.Detail)controller.detail("RET-1",7L).object();assertThat(result.refund().status()).isEqualTo("PROCESSED");verify(refunds,never()).save(any());verify(returns,never()).save(any());}
    @Test void listUsesOwnedQueryAndValidatesIdentityAndPaging(){when(returns.findOwnedByCustomer(eq(7L),any())).thenReturn(Page.empty());controller.list(7L,0);verify(returns).findOwnedByCustomer(eq(7L),any());assertThatThrownBy(()->controller.list(8L,0)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);assertThatThrownBy(()->controller.list(7L,-1)).isInstanceOf(IllegalArgumentException.class);}
}
