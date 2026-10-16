package com.example.eshop.order;

import com.example.eshop.order.service.impl.*;
import com.example.eshop.order.repository.*;
import com.example.eshop.order.mapper.*;
import com.example.eshop.common.security.LivePermissionService;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiLegacyOrderBoundaryTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void spoofedUserCannotReadOrderDetailHistoryOrLists() {
        var repo=mock(OrderRepository.class); var permissions=mock(LivePermissionService.class);
        var service=new OrderServiceImpl(mock(org.springframework.beans.factory.ObjectProvider.class),repo,
                mock(OrderCancelationRepository.class),mock(OrderMapper.class),mock(OrderItemMapper.class),mock(CheckoutCoordinator.class),permissions);
        var actor=new UsernamePasswordAuthenticationToken("buyer",null,List.of());actor.setDetails(Map.of("userId",7L));
        SecurityContextHolder.getContext().setAuthentication(actor);
        assertThatThrownBy(()->service.getOrderDetailByUserId(8L,30L)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(()->service.getUserOrders(8L,org.springframework.data.domain.PageRequest.of(0,10))).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(()->service.getOrderDetailHistory(8L,null,null,null,org.springframework.data.domain.PageRequest.of(0,10))).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verifyNoInteractions(repo,permissions);
    }
}
