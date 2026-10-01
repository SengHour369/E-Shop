package com.example.eshop.order.service;

import com.example.eshop.order.dto.request.GetOrderRequest;
import com.example.eshop.order.dto.request.OrderRequest;
import com.example.eshop.order.dto.response.ResponseErrorTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface OrderService {
    ResponseErrorTemplate getOrders(GetOrderRequest request);
    ResponseErrorTemplate getOrderById(Long id);
    ResponseErrorTemplate getOrderByNumber(String orderNumber);
    ResponseErrorTemplate getOrderDetailByUserId(Long userId, Long orderId);

    ResponseErrorTemplate createOrderFromCart(Long userId, OrderRequest request);
    ResponseErrorTemplate updateOrderStatus(Long id, String status);
    ResponseErrorTemplate cancelOrder(Long id, Long userId);

    Page<ResponseErrorTemplate> getUserOrders(Long userId, Pageable pageable);
    Page<ResponseErrorTemplate> getAllOrders(Pageable pageable);
    Page<ResponseErrorTemplate> getOrderDetailHistory(Long userId, String status, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    @Transactional(readOnly = true)
    ResponseErrorTemplate getOrderStatusSummary();
    ResponseErrorTemplate getOrderItemsByOrderId(Long orderId);
}
