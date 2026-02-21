package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.BadRequestException;
import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.*;
import com.example.learning_spring_security.Repository.*;
import com.example.learning_spring_security.Service.ServiceStructure.OrderService;

import com.example.learning_spring_security.ServiceMapper.OrderItemMapper;
import com.example.learning_spring_security.ServiceMapper.OrderMapper;
import com.example.learning_spring_security.dto.Request.OrderRequest;
import com.example.learning_spring_security.dto.Response.OrderResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final ProductSkuRepository productSkuRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public OrderResponse createOrderFromCart(Long userId, OrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user"));

        if (cart.getCartItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        Address shippingAddress = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + request.getAddressId()));

        // Verify user owns the address
        if (!addressRepository.isUserHasAddress(userId, request.getAddressId())) {
            throw new BadRequestException("Invalid shipping address");
        }

        // Create order
        OrderDetail order = OrderDetail.builder()
                .user(user)
                .orderNumber(generateOrderNumber())
                .orderDate(LocalDateTime.now())
                .status("PENDING")
                .subtotal(cart.getTotalPrice())
                .totalAmount(cart.getTotalPrice()) // Add shipping/tax calculation if needed
                .shippingAddress(shippingAddress)
                .build();

        // Create order items from cart items
        List<OrderItem> orderItems = cart.getCartItems().stream()
                .map(cartItem -> {
                    // Reduce stock
                    productSkuRepository.reduceStock(cartItem.getProductSku().getId(), cartItem.getQuantity());

                    return OrderItemMapper.toEntity(
                            order,
                            cartItem.getProductSku(),
                            cartItem.getQuantity()
                    );
                })
                .collect(Collectors.toList());

        order.setOrderItems(orderItems);

        // Create payment
        Payment payment = Payment.builder()
                .orderDetail(order)
                .paymentMethod(request.getPaymentMethod())
                .amount(order.getTotalAmount())
                .status("PENDING")
                .paymentDate(LocalDateTime.now())
                .build();

        order.setPayment(payment);

        // Save order
        OrderDetail savedOrder = orderRepository.save(order);

        // Clear cart
        cartItemRepository.deleteAllByCartId(cart.getId());
        cart.getCartItems().clear();
        cart.setTotalPrice(BigDecimal.ZERO);
        cart.setTotalItems(0);
        cartRepository.save(cart);

        return OrderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        OrderDetail order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return OrderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        OrderDetail order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));
        return OrderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return orderRepository.findByUserId(userId, pageable)
                .map(OrderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(OrderMapper::toResponse);
    }

    @Override
    public OrderResponse updateOrderStatus(Long id, String status) {
        OrderDetail order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        order.setStatus(status);

        // Update payment status if order is delivered/cancelled
        if ("DELIVERED".equals(status) && order.getPayment() != null) {
            order.getPayment().setStatus("COMPLETED");
        } else if ("CANCELLED".equals(status) && order.getPayment() != null) {
            order.getPayment().setStatus("REFUNDED");
            // Restore stock
            order.getOrderItems().forEach(item ->
                    productSkuRepository.increaseStock(item.getProductSku().getId(), item.getQuantity())
            );
        }

        OrderDetail updatedOrder = orderRepository.save(order);
        return OrderMapper.toResponse(updatedOrder);
    }

    @Override
    public OrderResponse cancelOrder(Long id, Long userId) {
        OrderDetail order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("User does not own this order");
        }

        if (!"PENDING".equals(order.getStatus())) {
            throw new BadRequestException("Only pending orders can be cancelled");
        }

        return updateOrderStatus(id, "CANCELLED");
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}