package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.GetOrderRequest;
import com.example.learning_spring_security.dto.Request.OrderRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

public interface OrderService {

    // Paginated – skip
    ResponseErrorTemplate getOrders(GetOrderRequest request);

    @Cacheable(value = "orders", key = "#id")
    ResponseErrorTemplate getOrderById(Long id);

    @Cacheable(value = "orders", key = "#orderNumber")
    ResponseErrorTemplate getOrderByNumber(String orderNumber);

    @Cacheable(value = "orders", key = "#userId + ':' + #orderId")
    ResponseErrorTemplate getOrderDetailByUserId(Long userId, Long orderId);

    @CacheEvict(value = {"orders", "carts"}, allEntries = true)
    ResponseErrorTemplate createOrderFromCart(Long userId, OrderRequest request);

    @CacheEvict(value = "orders", allEntries = true)
    ResponseErrorTemplate updateOrderStatus(Long id, String status);

    @CacheEvict(value = "orders", allEntries = true)
    ResponseErrorTemplate cancelOrder(Long id, Long userId);

    // Paginated – skip
    Page<ResponseErrorTemplate> getUserOrders(Long userId, Pageable pageable);

    // Paginated – skip
    Page<ResponseErrorTemplate> getAllOrders(Pageable pageable);

    // Paginated – skip
    Page<ResponseErrorTemplate> getOrderDetailHistory(Long userId, String status, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    @CacheEvict(value = "orders", allEntries = true)
    ResponseErrorTemplate createOrderWithBakongPayment(Long userId, OrderRequest request);

    @CacheEvict(value = "orders", key = "#orderId")
    ResponseErrorTemplate initiateBakongPayment(Long orderId);

    @CacheEvict(value = "orders", key = "#orderId")
    ResponseErrorTemplate verifyBakongPayment(Long orderId, String transactionId);

    @CacheEvict(value = "orders", key = "#orderNumber")
    ResponseErrorTemplate processBakongPaymentCallback(String orderNumber, String transactionId, String status);

    @Cacheable(value = "orders", key = "'summary'")
    @Transactional(readOnly = true)
    ResponseErrorTemplate getOrderStatusSummary();

    @Cacheable(value = "orders", key = "#orderId + ':items'")
    ResponseErrorTemplate getOrderItemsByOrderId(Long orderId);
}