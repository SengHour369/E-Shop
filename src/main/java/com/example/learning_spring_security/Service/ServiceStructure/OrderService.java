package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.OrderRequest;
import com.example.learning_spring_security.dto.Response.OrderResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    ResponseErrorTemplate createOrderFromCart(Long userId, OrderRequest request);
    ResponseErrorTemplate getOrderById(Long id);
    ResponseErrorTemplate getOrderByNumber(String orderNumber);
    Page<ResponseErrorTemplate> getUserOrders(Long userId, Pageable pageable);
    Page<ResponseErrorTemplate> getAllOrders(Pageable pageable);
    ResponseErrorTemplate updateOrderStatus(Long id, String status);
    ResponseErrorTemplate cancelOrder(Long id, Long userId);

    // Bakong Payment Integration Methods
    ResponseErrorTemplate createOrderWithBakongPayment(Long userId, OrderRequest request);
    ResponseErrorTemplate initiateBakongPayment(Long orderId);
    ResponseErrorTemplate verifyBakongPayment(Long orderId, String transactionId);
    ResponseErrorTemplate processBakongPaymentCallback(String orderNumber, String transactionId, String status);
}