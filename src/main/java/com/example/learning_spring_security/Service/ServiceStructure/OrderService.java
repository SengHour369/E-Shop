package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.GetOrderRequest;
import com.example.learning_spring_security.dto.Request.OrderRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

public interface OrderService {
    ResponseErrorTemplate getOrders(GetOrderRequest request);
    ResponseErrorTemplate getOrderById(Long id);
    ResponseErrorTemplate getOrderByNumber(String orderNumber);
    ResponseErrorTemplate getOrderDetailByUserId(Long userId, Long orderId);

    ResponseErrorTemplate createOrderFromCart(Long userId, OrderRequest request);
    ResponseErrorTemplate updateOrderStatus(Long id, String status);
    ResponseErrorTemplate cancelOrder(Long id, Long userId);

    // kept for backward compat
    Page<ResponseErrorTemplate> getUserOrders(Long userId, Pageable pageable);
    Page<ResponseErrorTemplate> getAllOrders(Pageable pageable);
    Page<ResponseErrorTemplate> getOrderDetailHistory(Long userId, String status, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    // Bakong Payment Integration Methods
    ResponseErrorTemplate createOrderWithBakongPayment(Long userId, OrderRequest request);
    ResponseErrorTemplate initiateBakongPayment(Long orderId);
    ResponseErrorTemplate verifyBakongPayment(Long orderId, String transactionId);
    ResponseErrorTemplate processBakongPaymentCallback(String orderNumber, String transactionId, String status);
}