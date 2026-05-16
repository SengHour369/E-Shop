package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.BadRequestException;
import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.*;
import com.example.learning_spring_security.Repository.*;
import com.example.learning_spring_security.dto.Request.OrderRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private ProductSkuRepository productSkuRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private Cart cart;
    private Address address;
    private OrderDetail order;
    private OrderRequest orderRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("testuser").build();
        address = Address.builder().id(1L).build();
        cart = Cart.builder()
                .id(1L)
                .user(user)
                .totalPrice(BigDecimal.valueOf(200))
                .totalItems(2)
                .cartItems(new ArrayList<>())
                .build();
        order = OrderDetail.builder()
                .id(1L)
                .user(user)
                .orderNumber("ORD-12345678")
                .status("PENDING")
                .totalAmount(BigDecimal.valueOf(200))
                .build();
        orderRequest = OrderRequest.builder()
                .addressId(1L)
                .paymentMethod("CREDIT_CARD")
                .build();
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void createOrderFromCart_ShouldCreateOrderSuccessfully() {
        // Given
        CartItem cartItem = CartItem.builder()
                .productSku(ProductSku.builder().id(1L).build())
                .quantity(2L)
                .build();
        cart.getCartItems().add(cartItem);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        when(addressRepository.isUserHasAddress(1L, 1L)).thenReturn(true);
        when(orderRepository.save(any(OrderDetail.class))).thenReturn(order);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // When
        ResponseErrorTemplate response = orderService.createOrderFromCart(1L, orderRequest);

        // Then
        assertThat(response).isNotNull();
        verify(orderRepository).save(any(OrderDetail.class));
        verify(cartRepository).save(cart);
    }

    @Test
    void createOrderFromCart_ShouldThrowException_WhenUserNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.createOrderFromCart(1L, orderRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 1");
    }

    @Test
    void createOrderFromCart_ShouldThrowException_WhenCartNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.createOrderFromCart(1L, orderRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Cart not found for user");
    }

    @Test
    void createOrderFromCart_ShouldThrowException_WhenCartIsEmpty() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrderFromCart(1L, orderRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cart is empty");
    }

    @Test
    void createOrderFromCart_ShouldThrowException_WhenAddressNotFound() {
        // Given
        cart.getCartItems().add(CartItem.builder().build());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(addressRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.createOrderFromCart(1L, orderRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Address not found with id: 1");
    }

    @Test
    void createOrderFromCart_ShouldThrowException_WhenInvalidAddress() {
        // Given
        cart.getCartItems().add(CartItem.builder().build());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        when(addressRepository.isUserHasAddress(1L, 1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> orderService.createOrderFromCart(1L, orderRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid shipping address");
    }

    @Test
    void getOrderById_ShouldReturnOrder_WhenExists() {
        // Given
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

        // When
        ResponseErrorTemplate response = orderService.getOrderById(1L);

        // Then
        assertThat(response).isNotNull();
        verify(orderRepository).findByIdWithItems(1L);
    }

    @Test
    void getOrderById_ShouldThrowException_WhenNotFound() {
        // Given
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.getOrderById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found with id: 1");
    }

    @Test
    void getOrderByNumber_ShouldReturnOrder_WhenExists() {
        // Given
        when(orderRepository.findByOrderNumber("ORD-12345678")).thenReturn(Optional.of(order));

        // When
        ResponseErrorTemplate response = orderService.getOrderByNumber("ORD-12345678");

        // Then
        assertThat(response).isNotNull();
        verify(orderRepository).findByOrderNumber("ORD-12345678");
    }

    @Test
    void getOrderByNumber_ShouldThrowException_WhenNotFound() {
        // Given
        when(orderRepository.findByOrderNumber("ORD-12345678")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.getOrderByNumber("ORD-12345678"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found with number: ORD-12345678");
    }

    @Test
    void getUserOrders_ShouldReturnPagedOrders() {
        // Given
        Page<OrderDetail> orderPage = new PageImpl<>(List.of(order));
        when(userRepository.existsById(1L)).thenReturn(true);
        when(orderRepository.findByUserId(1L, pageable)).thenReturn(orderPage);

        // When
        Page<ResponseErrorTemplate> response = orderService.getUserOrders(1L, pageable);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    void getUserOrders_ShouldThrowException_WhenUserNotFound() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> orderService.getUserOrders(1L, pageable))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 1");
    }

    @Test
    void getAllOrders_ShouldReturnPagedOrders() {
        // Given
        Page<OrderDetail> orderPage = new PageImpl<>(List.of(order));
        when(orderRepository.findAll(pageable)).thenReturn(orderPage);

        // When
        Page<ResponseErrorTemplate> response = orderService.getAllOrders(pageable);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    void updateOrderStatus_ShouldUpdateToDelivered() {
        // Given
        Payment payment = Payment.builder().status("PENDING").build();
        order.setPayment(payment);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderDetail.class))).thenReturn(order);

        // When
        ResponseErrorTemplate response = orderService.updateOrderStatus(1L, "DELIVERED");

        // Then
        assertThat(response).isNotNull();
        assertThat(order.getPayment().getStatus()).isEqualTo("COMPLETED");
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrderStatus_ShouldUpdateToCancelled() {
        // Given
        Payment payment = Payment.builder().status("PENDING").build();
        order.setPayment(payment);
        OrderItem orderItem = OrderItem.builder()
                .productSku(ProductSku.builder().id(1L).build())
                .quantity(2L)
                .build();
        order.setOrderItems(List.of(orderItem));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderDetail.class))).thenReturn(order);

        // When
        ResponseErrorTemplate response = orderService.updateOrderStatus(1L, "CANCELLED");

        // Then
        assertThat(response).isNotNull();
        assertThat(order.getPayment().getStatus()).isEqualTo("REFUNDED");
        verify(productSkuRepository).increaseStock(1L, 2L);
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrderStatus_ShouldThrowException_WhenOrderNotFound() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, "DELIVERED"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found with id: 1");
    }

    @Test
    void cancelOrder_ShouldCancelSuccessfully() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderDetail.class))).thenReturn(order);

        // When
        ResponseErrorTemplate response = orderService.cancelOrder(1L, 1L);

        // Then
        assertThat(response).isNotNull();
        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrder_ShouldThrowException_WhenUserDoesNotOwnOrder() {
        // Given
        User otherUser = User.builder().id(2L).build();
        order.setUser(otherUser);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("User does not own this order");
    }

    @Test
    void cancelOrder_ShouldThrowException_WhenOrderNotPending() {
        // Given
        order.setStatus("SHIPPED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only pending orders can be cancelled");
    }
}
