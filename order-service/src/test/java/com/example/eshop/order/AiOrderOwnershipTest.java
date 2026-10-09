package com.example.eshop.order;
import com.example.eshop.order.controller.AiOrderController;
import com.example.eshop.order.repository.OrderRepository;
import com.example.eshop.order.service.OrderService;
import com.example.eshop.order.model.OrderDetail;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class AiOrderOwnershipTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void otherCustomersOrderNeverReachesBusinessResponse() {
        var repo=mock(OrderRepository.class);
        var service=mock(com.example.eshop.common.security.LivePermissionService.class);
        var order=new OrderDetail(); order.setId(1L); order.setUserId(8L);
        when(repo.findByOrderNumber("ORD-1")).thenReturn(Optional.of(order));
        var auth=new UsernamePasswordAuthenticationToken("user",null,List.of()); auth.setDetails(Map.of("userId",7L));
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThatThrownBy(()->new AiOrderController(repo,service,mock(OrderService.class)).get("ORD-1")).hasMessage("Order not found");
        verify(service).current();
    }
}
