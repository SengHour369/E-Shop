package com.example.eshop.order.service.impl;

import com.example.eshop.common.exception.BusinessLogicException;
import com.example.eshop.common.exception.ResourceNotFoundException;
import com.example.eshop.order.constant.CancelReason;
import com.example.eshop.order.constant.CancelStatus;
import com.example.eshop.order.constant.OrderStatus;
import com.example.eshop.order.model.OrderCancelation;
import com.example.eshop.order.model.OrderDetail;
import com.example.eshop.order.model.OrderItem;
import com.example.eshop.order.repository.OrderCancelationRepository;
import com.example.eshop.order.repository.OrderRepository;
import com.example.eshop.order.service.OrderService;

import com.example.eshop.order.mapper.OrderItemMapper;
import com.example.eshop.order.mapper.OrderMapper;
import com.example.eshop.order.dto.request.GetOrderRequest;
import com.example.eshop.order.dto.request.OrderRequest;
import com.example.eshop.order.dto.response.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** Order queries and status changes; CheckoutCoordinator owns durable catalog orchestration. */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderCancelationRepository orderCancelationRepository;

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    private final CheckoutCoordinator checkout;

    @Override
    public ResponseErrorTemplate createOrderFromCart(Long userId, OrderRequest request) {
        return checkout.create(userId, request);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getOrders(GetOrderRequest request) {
        log.info("getOrders: criteriaType={}, criteriaValue={}, page={}, size={}",
                request.getCriteriaType(), request.getCriteriaValue(), request.getPage(), request.getSize());

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                request.getPage() - 1,
                request.getSize(),
                org.springframework.data.domain.Sort.by("orderDate").descending()
        );

        Integer type = request.getCriteriaType();
        String value = request.getCriteriaValue();

        org.springframework.data.domain.Page<OrderDetail> page;
        String successMsg;

        if (type == null || type == 0 || value == null || value.isBlank()) {
            page = orderRepository.findAll(pageable);
            successMsg = "Retrieved all orders";

        } else if (type == 1) {
            page = orderRepository.findByUserId(Long.parseLong(value), pageable);
            successMsg = "Retrieved orders by user";

        } else if (type == 2) {
            page = orderRepository.findByStatus(value, pageable);
            successMsg = "Retrieved orders by status";

        } else if (type == 3) {
            String[] parts = value.split(":");
            if (parts.length != 2) {
                throw new BusinessLogicException("criteriaValue for type 3 must be 'userId:status'");
            }
            page = orderRepository.findOrderDetailHistory(
                    Long.parseLong(parts[0].trim()),
                    parts[1].trim(),
                    null, null,
                    pageable
            );
            successMsg = "Retrieved orders by user and status";

        } else if (type == 4) {
            String[] parts = value.split(":");
            if (parts.length != 3) {
                throw new BusinessLogicException("criteriaValue for type 4 must be 'userId:startDate:endDate' (yyyy-MM-ddTHH:mm:ss)");
            }
            page = orderRepository.findOrderDetailHistory(
                    Long.parseLong(parts[0].trim()),
                    null,
                    java.time.LocalDateTime.parse(parts[1].trim()),
                    java.time.LocalDateTime.parse(parts[2].trim()),
                    pageable
            );
            successMsg = "Retrieved orders by user and date range";

        } else {
            page = orderRepository.findAll(pageable);
            successMsg = "Retrieved all orders";
        }

        java.util.List<OrderResponse> payload = page.getContent()
                .stream()
                .map(o -> (OrderResponse) orderMapper.toResponse(o).object())
                .toList();

        OrderPageResponse pageResponse = OrderPageResponse.builder()
                .payload(payload)
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber() + 1)
                .pageSize(page.getSize())
                .build();

        String message = page.isEmpty() ? "No orders found" : successMsg;
        return ResponseErrorTemplate.success(message, pageResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getOrderById(Long id) {
        OrderDetail order = orderRepository.findByIdWithFullDetail(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getOrderByNumber(String orderNumber) {
        OrderDetail order = orderRepository.findByOrderNumberWithFullDetail(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseErrorTemplate> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable)
                .map(orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseErrorTemplate> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(orderMapper::toResponse);
    }

    @Override
    @Transactional
    public ResponseErrorTemplate updateOrderStatus(Long id, String status) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ADMIN")))
            throw new org.springframework.security.access.AccessDeniedException("ADMIN required");
        OrderDetail order = orderRepository.lockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if ("CHECKOUT_PENDING".equals(order.getStatus()) || "FAILED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus()))
            throw new BusinessLogicException("Order is not eligible for a status change");
        if (!java.util.Set.of("PENDING", "CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED").contains(status))
            throw new BusinessLogicException("Unsupported order status");
        if (OrderStatus.CANCELLED.equals(status) && !OrderStatus.PENDING.equals(order.getStatus()))
            throw new BusinessLogicException("Only pending orders can be cancelled");
        order.setStatus(status);
        if (OrderStatus.CANCELLED.equals(status) && order.getCheckoutKey() != null) order.setCatalogCompleted(false);

        if (OrderStatus.CANCELLED.equals(status)) {
            // Checkout recovery releases the catalog reservation after this transaction commits.
            log.info("Order {} cancelled — catalog-service should restock its items", order.getOrderNumber());
        }

        OrderDetail updatedOrder = orderRepository.save(order);

        // TODO: cross-service notification (e.g. via Feign or an event) so a notification service
        // can email the customer about the status change.

        return orderMapper.toResponse(updatedOrder);
    }

    @Override
    @Transactional
    public ResponseErrorTemplate cancelOrder(Long id, Long userId) {
        com.example.eshop.common.security.CurrentCustomer.require(userId);
        OrderDetail order = orderRepository.lockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessLogicException("User does not own this order");
        }

        if (!OrderStatus.PENDING.equals(order.getStatus())) {
            throw new BusinessLogicException("Only pending orders can be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        if (order.getCheckoutKey() != null) order.setCatalogCompleted(false);
        // Checkout recovery releases stock and promotion usage idempotently.
        OrderDetail cancelled = orderRepository.save(order);

        String actor = currentUsername();
        orderCancelationRepository.save(OrderCancelation.builder()
                .cancelationId(generateCancelationId())
                .orderId(cancelled.getId())
                .orderNo(cancelled.getOrderNumber())
                .customerId(cancelled.getUserId())
                // TODO: cross-service call via Feign to auth-service to resolve the customer's display name.
                .customerName(null)
                .cancelReason(CancelReason.CUSTOMER_REQUESTED)
                .cancelStatus(CancelStatus.CANCELED)
                .cancelSource("CUSTOMER")
                .cancelDate(LocalDateTime.now())
                .amount(cancelled.getTotalAmount())
                .currency("USD")
                .remark("Cancelled by customer")
                .createdBy(actor)
                .build());

        return orderMapper.toResponse(cancelled);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getOrderDetailByUserId(Long userId, Long orderId) {
        OrderDetail order = orderRepository.findByIdAndUserIdWithFullDetail(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId + " for user: " + userId));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseErrorTemplate> getOrderDetailHistory(Long userId, String status, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return orderRepository.findOrderDetailHistory(userId, status, startDate, endDate, pageable)
                .map(orderMapper::toResponse);
    }

    private String generateCancelationId() {
        String cancelationId;
        do {
            cancelationId = "CNL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (orderCancelationRepository.existsByCancelationId(cancelationId));
        return cancelationId;
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseErrorTemplate getOrderStatusSummary() {
        OrderStatusSummaryResponse response = orderRepository.getOrderStatusSummary();
        return ResponseErrorTemplate.success("Order status summary retrieved successfully", response);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getOrderItemsByOrderId(Long orderId) {
        OrderDetail order = orderRepository.findByIdWithFullDetail(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        List<OrderItemResponse> items = order.getOrderItems()
                .stream()
                .map(orderItemMapper::toResponse)
                .toList();

        return ResponseErrorTemplate.success("Order items retrieved successfully", items);
    }
}
