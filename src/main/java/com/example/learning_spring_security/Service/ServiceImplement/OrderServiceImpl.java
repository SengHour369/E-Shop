package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.BadRequestException;
import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.*;
import com.example.learning_spring_security.Repository.*;
import com.example.learning_spring_security.Service.ServiceStructure.OrderService;

import com.example.learning_spring_security.ServiceMapper.OrderItemMapper;
import com.example.learning_spring_security.ServiceMapper.OrderMapper;
import com.example.learning_spring_security.ServiceMapper.PaymentMapper;
import com.example.learning_spring_security.dto.Request.OrderRequest;
import com.example.learning_spring_security.dto.Response.OrderResponse;

import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import com.example.learning_spring_security.Bakong.service.impl.dto.BakongRequest;
import com.example.learning_spring_security.Bakong.service.impl.dto.BakongResponse;
import com.example.learning_spring_security.Bakong.service.impl.service.BakongService;
import com.example.learning_spring_security.Bakong.service.impl.dto.CheckTransactionRequest;
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final ProductSkuRepository productSkuRepository;

    // Bakong Payment Service
    private final BakongService bakongService;

    @Override
    public ResponseErrorTemplate createOrderFromCart(Long userId, OrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user"));

        if (cart.getCartItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }
        Address shippingAddress = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + request.getAddressId()));

        if (!addressRepository.isUserHasAddress(userId, request.getAddressId())) {
            throw new BadRequestException("Invalid shipping address");
        }

        OrderDetail order = OrderDetail.builder()
                .user(user)
                .orderNumber(generateOrderNumber())
                .orderDate(LocalDateTime.now())
                .status("PENDING")
                .totalAmount(cart.getTotalPrice())
                .shippingAddress(shippingAddress)
                .build();

        List<OrderItem> orderItems = cart.getCartItems().stream()
                .map(cartItem -> {
                    productSkuRepository.reduceStock(cartItem.getProductSku().getId()
                            , cartItem.getQuantity());
                    return OrderItemMapper.toEntity(
                            order,
                            cartItem.getProductSku(),
                            cartItem.getQuantity()
                    );
                })
                .collect(Collectors.toList());
        order.setOrderItems(orderItems);

        Payment payment = Payment.builder()
                .orderDetail(order)
                .paymentMethod(request.getPaymentMethod())
                .amount(order.getTotalAmount())
                .status("PENDING")
                .paymentDate(LocalDateTime.now())
                .build();

        order.setPayment(payment);
        OrderDetail savedOrder = orderRepository.save(order);

        cart.getCartItems().clear();
        cart.setTotalPrice(BigDecimal.ZERO);
        cart.setTotalItems(0);
        cartRepository.save(cart);
        return OrderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getOrderById(Long id) {
        OrderDetail order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return OrderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getOrderByNumber(String orderNumber) {
        OrderDetail order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));
        return OrderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseErrorTemplate> getUserOrders(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return orderRepository.findByUserId(userId, pageable)
                .map(OrderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseErrorTemplate> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(OrderMapper::toResponse);
    }

    @Override
    public ResponseErrorTemplate updateOrderStatus(Long id, String status) {
        OrderDetail order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        order.setStatus(status);


        if ("DELIVERED".equals(status) && order.getPayment() != null) {
            order.getPayment().setStatus("COMPLETED");
        } else if ("CANCELLED".equals(status) && order.getPayment() != null) {
            order.getPayment().setStatus("REFUNDED");

            order.getOrderItems().forEach(item ->
                    productSkuRepository.increaseStock(item.getProductSku().getId(), item.getQuantity())
            );
        }

        OrderDetail updatedOrder = orderRepository.save(order);
        return OrderMapper.toResponse(updatedOrder);
    }

    @Override
    public ResponseErrorTemplate cancelOrder(Long id, Long userId) {
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

    // ===========================================
    // BAKONG PAYMENT INTEGRATION METHODS
    // ===========================================

    @Override
    public ResponseErrorTemplate createOrderWithBakongPayment(Long userId, OrderRequest request) {
        // Validate payment method is BAKONG
        if (!"BAKONG".equalsIgnoreCase(request.getPaymentMethod())) {
            throw new BadRequestException("This method is only for Bakong payments");
        }

        // Create order using existing logic
        ResponseErrorTemplate orderResponse = createOrderFromCart(userId, request);

        // Extract order details from response
        OrderResponse orderData = (OrderResponse) orderResponse.object();

        // Initiate Bakong payment immediately
        try {
            BakongRequest bakongRequest = BakongRequest.builder()
                    .currency("KHR")
                    .amount(orderData.getTotalAmount().doubleValue())
                    .merchantName("E_Shop")
                    .merchantCity("PHNOM PENH")
                    .merchantId("ESHOP001")
                    .acquiringBank("BAKONG")
                    .billNumber(orderData.getOrderNumber())
                    .storeLabel("E_SHOP_STORE")
                    .terminalLabel("TERMINAL_01")
                    .mobileNumber("012345678")
                    .purposeOfTransaction("Payment " + orderData.getOrderNumber())
                    .expirationTimestamp(15) // 15 minutes
                    .build();

            KHQRResponse<KHQRData> bakongResponse = bakongService.generateQR(bakongRequest);
           System.out.println("Failed to generate QR code: " + bakongResponse.getKHQRStatus().getMessage());
            if (bakongResponse != null
                    && bakongResponse.getKHQRStatus() != null
                    && bakongResponse.getKHQRStatus().getCode() == 0) {

                // Update payment with QR code and transaction info
                OrderDetail order = orderRepository.findByOrderNumber(orderData.getOrderNumber())
                        .orElseThrow(() -> new ResourceNotFoundException("Order not found after creation"));

                Payment payment = order.getPayment();
                payment.setTransactionId("BAKONG-" + orderData.getOrderNumber());
                payment.setPaymentProvider("BAKONG");
                payment.setPaymentProviderResponse(bakongResponse.getData().getQr());
                 order.setPayment(payment);
                orderRepository.save(order);
                 orderData.setPayment(PaymentMapper.toResponse(payment));
                // Return enhanced response with QR code
                orderData.setQrCode(bakongResponse.getData().getQr());
                orderData.setPaymentUrl("bakong://payment?qr=" + bakongResponse.getData());
                  }

        } catch (Exception e) {
            // Log error but don't fail order creation
            System.err.println("Failed to generate Bakong QR: " + e.getMessage());
        }

        return orderResponse;
    }

    @Override
    public ResponseErrorTemplate initiateBakongPayment(Long orderId) {
        OrderDetail order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!"BAKONG".equalsIgnoreCase(order.getPayment().getPaymentMethod())) {
            throw new BadRequestException("Order does not use Bakong payment method");
        }

        if (!"PENDING".equals(order.getStatus())) {
            throw new BadRequestException("Order is not in pending status");
        }

        try {
            BakongRequest bakongRequest = BakongRequest.builder()
                    .currency("KHR")
                    .amount(order.getTotalAmount().doubleValue())
                    .merchantName("E_Shop")
                    .merchantCity("PHNOM PENH")
                    .merchantId("ESHOP001")
                    .acquiringBank("NBC")
                    .billNumber(order.getOrderNumber())
                    .storeLabel("E_SHOP_STORE")
                    .terminalLabel("TERMINAL_01")
                    .mobileNumber("012345678")
                    .purposeOfTransaction("Payment for Order " + order.getOrderNumber())
                    .expirationTimestamp(15)
                    .build();

            KHQRResponse<KHQRData> bakongResponse = bakongService.generateQR(bakongRequest);

            if (bakongResponse != null
                    && bakongResponse.getKHQRStatus() != null
                    && bakongResponse.getKHQRStatus().getCode() == 0) {
                // Update payment record
                Payment payment = order.getPayment();
                payment.setTransactionId("BAKONG-" + order.getOrderNumber());
                payment.setPaymentProvider("BAKONG");
                payment.setPaymentProviderResponse(bakongResponse.getData().getQr());

                orderRepository.save(order);

                return ResponseErrorTemplate.builder()
                        .message("Bakong payment initiated successfully")
                        .object(Map.of(
                                "orderId", order.getId(),
                                "orderNumber", order.getOrderNumber(),
                                "qrCode", bakongResponse.getData().getQr(),
                                "paymentUrl", "bakong://payment?qr=" +bakongResponse.getData().getQr(),
                                "amount", order.getTotalAmount(),
                                "expiresIn", "15 minutes"
                        ))
                        .build();
            } else {
                throw new RuntimeException("Failed to generate QR code: " + bakongResponse);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to initiate Bakong payment: " + e.getMessage(), e);
        }
    }

    @Override
    public ResponseErrorTemplate verifyBakongPayment(Long orderId, String transactionId) {
        OrderDetail order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!"BAKONG".equalsIgnoreCase(order.getPayment().getPaymentMethod())) {
            throw new BadRequestException("Order does not use Bakong payment method");
        }

        try {
            // Extract MD5 from transaction ID or use provided transactionId as MD5
            String md5 = transactionId.contains("-") ?
                    transactionId.split("-")[1] : transactionId;

            BakongResponse bakongResponse = bakongService.checkTransactionByMD5(
                    new CheckTransactionRequest(md5)
            );

            if ("SUCCESS".equals(bakongResponse.getStatus())) {
                // Update order and payment status
                order.setStatus("CONFIRMED");
                order.getPayment().setStatus("COMPLETED");
                order.getPayment().setTransactionId(transactionId);

                orderRepository.save(order);

                return ResponseErrorTemplate.builder()
                        .message("Payment verified successfully")
                        .object(Map.of(
                                "orderId", order.getId(),
                                "orderNumber", order.getOrderNumber(),
                                "status", "CONFIRMED",
                                "paymentStatus", "COMPLETED"
                        ))
                        .build();
            } else {
                return ResponseErrorTemplate.builder()
                        .message("Payment verification failed")
                        .object(Map.of(
                                "orderId", order.getId(),
                                "status", "PAYMENT_FAILED",
                                "error", bakongResponse.getMessage()
                        ))
                        .build();
            }

        } catch (Exception e) {
            return ResponseErrorTemplate.builder()
                    .message("Payment verification error")
                    .object(Map.of(
                            "orderId", order.getId(),
                            "error", e.getMessage()
                    ))
                    .build();
        }
    }

    @Override
    public ResponseErrorTemplate processBakongPaymentCallback(String orderNumber, String transactionId, String status) {
        OrderDetail order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));

        if (!"BAKONG".equalsIgnoreCase(order.getPayment().getPaymentMethod())) {
            throw new BadRequestException("Order does not use Bakong payment method");
        }

        // Update payment and order status based on callback
        Payment payment = order.getPayment();
        payment.setTransactionId(transactionId);

        switch (status.toUpperCase()) {
            case "SUCCESS":
            case "COMPLETED":
                order.setStatus("CONFIRMED");
                payment.setStatus("COMPLETED");
                break;
            case "FAILED":
            case "CANCELLED":
                order.setStatus("CANCELLED");
                payment.setStatus("FAILED");
                // Restore inventory
                order.getOrderItems().forEach(item ->
                        productSkuRepository.increaseStock(item.getProductSku().getId(), item.getQuantity())
                );
                break;
            case "PENDING":
            default:
                order.setStatus("PENDING");
                payment.setStatus("PENDING");
                break;
        }

        orderRepository.save(order);

        return ResponseErrorTemplate.builder()
                .message("Payment callback processed successfully")
                .object(Map.of(
                        "orderNumber", orderNumber,
                        "transactionId", transactionId,
                        "orderStatus", order.getStatus(),
                        "paymentStatus", payment.getStatus()
                ))
                .build();
    }
}