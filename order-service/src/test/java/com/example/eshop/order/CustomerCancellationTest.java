package com.example.eshop.order;

import com.example.eshop.order.dto.request.CancelOrderRequest;
import com.example.eshop.order.model.*;
import com.example.eshop.order.mapper.*;
import com.example.eshop.order.repository.*;
import com.example.eshop.order.service.impl.*;
import com.example.eshop.common.automation.AutomationEvents;
import jakarta.validation.Validation;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class CustomerCancellationTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void validatesReasonAndNotes() {
        try(var factory=Validation.buildDefaultValidatorFactory()) {
            var validator=factory.getValidator();
            assertThat(validator.validate(new CancelOrderRequest("OTHER","x".repeat(500)))).isEmpty();
            assertThat(validator.validate(new CancelOrderRequest("INVALID",""))).isNotEmpty();
            assertThat(validator.validate(new CancelOrderRequest("OTHER","x".repeat(501)))).isNotEmpty();
            assertThat(validator.validate(new CancelOrderRequest(null,null))).isEmpty();
        }
    }
    @Test void savesSelectedReasonInExistingAuditRecordAndCancelsOnlyOwnedPendingOrders() {
        var repo=mock(OrderRepository.class);var cancellations=mock(OrderCancelationRepository.class);
        @SuppressWarnings("unchecked") ObjectProvider<AutomationEvents> events=mock(ObjectProvider.class);
        var service=new OrderServiceImpl(events,repo,cancellations,mock(OrderMapper.class),mock(OrderItemMapper.class),mock(CheckoutCoordinator.class),mock(com.example.eshop.order.automation.QrCancellationGuard.class),mock(com.example.eshop.common.security.LivePermissionService.class));
        var auth=new UsernamePasswordAuthenticationToken("customer",null,List.of());auth.setDetails(Map.of("userId",7L));SecurityContextHolder.getContext().setAuthentication(auth);
        var order=new OrderDetail();order.setId(1L);order.setUserId(7L);order.setOrderNumber("ORD-1");order.setStatus("PENDING");order.setTotalAmount(BigDecimal.TEN);
        when(repo.lockById(1L)).thenReturn(Optional.of(order));when(repo.save(order)).thenReturn(order);
        service.cancelOrder(1L,7L,new CancelOrderRequest("BETTER_PRICE","  Cheaper elsewhere  "));
        var record=org.mockito.ArgumentCaptor.forClass(OrderCancelation.class);verify(cancellations).save(record.capture());
        assertThat(order.getStatus()).isEqualTo("CANCELLED");assertThat(record.getValue().getRemark()).isEqualTo("Found a better price: Cheaper elsewhere");
        clearInvocations(cancellations);order.setStatus("SHIPPED");
        assertThatThrownBy(()->service.cancelOrder(1L,7L,new CancelOrderRequest("OTHER",""))).hasMessage("Only pending orders can be cancelled");verifyNoInteractions(cancellations);
        order.setUserId(8L);order.setStatus("PENDING");
        assertThatThrownBy(()->service.cancelOrder(1L,7L,new CancelOrderRequest("OTHER",""))).hasMessage("User does not own this order");verifyNoInteractions(cancellations);
    }
    @Test void legacyBodyRetainsOriginalRemark() { assertThat(new CancelOrderRequest(null,null).auditRemark()).isEqualTo("Cancelled by customer"); }
}
