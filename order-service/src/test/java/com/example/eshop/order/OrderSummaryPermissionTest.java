package com.example.eshop.order;

import com.example.eshop.common.security.LivePermissionService;
import com.example.eshop.order.repository.OrderRepository;
import com.example.eshop.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OrderSummaryPermissionTest {
    @Mock LivePermissionService permissions;
    @Mock OrderRepository orders;
    @InjectMocks OrderServiceImpl service;

    @Test void summaryUsesTheSamePermissionAsOrderListing() {
        service.getOrderStatusSummary();
        var ordered = inOrder(permissions, orders);
        ordered.verify(permissions).require("ORDER_VIEW_ALL");
        ordered.verify(orders).getOrderStatusSummary();
        verifyNoMoreInteractions(permissions);
    }

    @Test void deniedCallerCannotReadSummaryData() {
        doThrow(new AccessDeniedException("Required permission missing"))
                .when(permissions).require("ORDER_VIEW_ALL");
        assertThatThrownBy(() -> service.getOrderStatusSummary()).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(orders);
    }
}
