package com.example.eshop.order.automation;

import com.example.eshop.common.automation.VerifiedPaymentSnapshot;
import com.example.eshop.order.model.OrderDetail;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;
import java.math.BigDecimal;
import java.time.Instant;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class QrCancellationGuardTest {
    PaymentAutomationClient client=mock(PaymentAutomationClient.class);
    @SuppressWarnings("unchecked") ObjectProvider<PaymentAutomationClient> provider=mock(ObjectProvider.class);
    QrCancellationGuard guard=new QrCancellationGuard(provider);
    OrderDetail order=new OrderDetail();
    @BeforeEach void setup(){order.setId(1L);order.setUserId(2L);order.setPaymentMethod("ABA");order.setTotalAmount(BigDecimal.TEN);when(provider.getIfAvailable()).thenReturn(client);}
    VerifiedPaymentSnapshot snapshot(String status,Instant verified,Instant expiry){return new VerifiedPaymentSnapshot(3,1,2,BigDecimal.TEN,"USD",status,verified==null?null:"a".repeat(64),verified,expiry);}
    @Test void unknownOrPaidCannotCancel(){
        for(String status:java.util.List.of("PENDING","SUCCESS","REVIEW_REQUIRED")){
            when(client.forOrder(1)).thenReturn(snapshot(status,null,Instant.now().minusSeconds(120)));
            assertThatThrownBy(()->guard.requireUnpaid(order)).hasMessageContaining("reconciliation");
        }
    }
    @Test void failedWithProofOrWithoutExpiryCannotCancel(){
        when(client.forOrder(1)).thenReturn(snapshot("FAILED",Instant.now(),Instant.now().minusSeconds(120)));
        assertThatThrownBy(()->guard.requireUnpaid(order)).isInstanceOf(RuntimeException.class);
        when(client.forOrder(1)).thenReturn(snapshot("FAILED",null,null));
        assertThatThrownBy(()->guard.requireUnpaid(order)).isInstanceOf(RuntimeException.class);
    }
    @Test void authoritativeUnpaidExpiredAttemptCanCancel(){when(client.forOrder(1)).thenReturn(snapshot("FAILED",null,Instant.now().minusSeconds(120)));assertThatCode(()->guard.requireUnpaid(order)).doesNotThrowAnyException();}
    @Test void providerOutageCannotCancel(){when(client.forOrder(1)).thenThrow(new IllegalStateException("offline"));assertThatThrownBy(()->guard.requireUnpaid(order)).hasMessage("offline");}
    @Test void cashOnDeliveryRetainsCancellation(){order.setPaymentMethod("CASH_ON_DELIVERY");guard.requireUnpaid(order);verifyNoInteractions(client);}
    @Test void snapshotMustMatchOrder(){when(client.forOrder(1)).thenReturn(new VerifiedPaymentSnapshot(3,99,2,BigDecimal.TEN,"USD","FAILED",null,null,Instant.now().minusSeconds(120)));assertThatThrownBy(()->guard.requireUnpaid(order)).isInstanceOf(RuntimeException.class);}
}
