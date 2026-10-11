package com.example.eshop.order.automation;
import com.example.eshop.common.automation.*;
import com.example.eshop.common.audit.AuditLogService;
import com.example.eshop.order.model.OrderDetail;
import com.example.eshop.order.repository.OrderRepository;
import com.example.eshop.order.service.impl.CatalogAccess;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class OrderAutomationTest {
    OrderRepository orders=mock(OrderRepository.class);PaymentAutomationClient payments=mock(PaymentAutomationClient.class);
    CatalogAccess catalog=mock(CatalogAccess.class);AutomationEvents events=mock(AutomationEvents.class);
    OrderAutomation handler=new OrderAutomation(orders,payments,catalog,events,mock(AuditLogService.class));OrderDetail order;
    @BeforeEach void setup(){order=new OrderDetail();order.setId(1L);order.setUserId(2L);order.setTotalAmount(new BigDecimal("10.00"));order.setStatus("PENDING");when(orders.lockById(1L)).thenReturn(Optional.of(order));}
    CommerceEvent event(CommerceEvent.Type type){return new CommerceEvent(1,UUID.randomUUID(),type,1,2,new BigDecimal("10.00"),"USD","BAKONG",3L,"test-request",null,Instant.now());}
    VerifiedPaymentSnapshot payment(String status,Instant proof,BigDecimal amount){return new VerifiedPaymentSnapshot(3,1,2,amount,"USD",status,proof==null?null:"a".repeat(64),proof,Instant.now().minusSeconds(120));}
    @Test void refusesUnverifiedSuccess(){when(payments.get(3)).thenReturn(payment("SUCCESS",null,BigDecimal.TEN));assertThatThrownBy(()->handler.handle(event(CommerceEvent.Type.PAYMENT_VERIFIED))).isInstanceOf(IllegalStateException.class);verifyNoInteractions(catalog);assertThat(order.getStatus()).isEqualTo("PENDING");}
    @Test void refusesMismatchedAmount(){when(payments.get(3)).thenReturn(payment("SUCCESS",Instant.now(),BigDecimal.ONE));assertThatThrownBy(()->handler.handle(event(CommerceEvent.Type.PAYMENT_VERIFIED))).isInstanceOf(IllegalStateException.class);verifyNoInteractions(catalog);}
    @Test void duplicateVerifiedEventsConfirmStockOnce(){when(payments.get(3)).thenReturn(payment("SUCCESS",Instant.now(),BigDecimal.TEN));handler.handle(event(CommerceEvent.Type.PAYMENT_VERIFIED));handler.handle(event(CommerceEvent.Type.PAYMENT_VERIFIED));verify(catalog,times(1)).confirm(1L);assertThat(order.getStatus()).isEqualTo("PROCESSING");}
    @Test void cancelledOrderIsNeverRevivedByLatePayment(){order.setStatus("CANCELLED");when(payments.get(3)).thenReturn(payment("SUCCESS",Instant.now(),BigDecimal.TEN));handler.handle(event(CommerceEvent.Type.PAYMENT_VERIFIED));verifyNoInteractions(catalog);verify(events).review(eq(1L),eq("PAID_ORDER_STATE_CONFLICT"),anyString());assertThat(order.getStatus()).isEqualTo("CANCELLED");}
    @Test void expiredVerifiedNegativeReleasesOnlyOnce(){when(payments.get(3)).thenReturn(payment("FAILED",null,BigDecimal.TEN));handler.handle(event(CommerceEvent.Type.PAYMENT_EXPIRED));handler.handle(event(CommerceEvent.Type.PAYMENT_EXPIRED));verify(catalog,times(1)).release(1L);assertThat(order.getStatus()).isEqualTo("CANCELLED");}
    @Test void inventoryOutageDoesNotAdvanceOrder(){when(payments.get(3)).thenReturn(payment("SUCCESS",Instant.now(),BigDecimal.TEN));doThrow(new IllegalStateException("offline")).when(catalog).confirm(1L);assertThatThrownBy(()->handler.handle(event(CommerceEvent.Type.PAYMENT_VERIFIED))).isInstanceOf(IllegalStateException.class);assertThat(order.getStatus()).isEqualTo("PENDING");verifyNoInteractions(events);}
}
