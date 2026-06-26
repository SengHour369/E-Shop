package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.BadRequestException;
import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.OrderDetail;
import com.example.learning_spring_security.Model.Payment;
import com.example.learning_spring_security.Repository.OrderRepository;
import com.example.learning_spring_security.Repository.PaymentRepository;
import com.example.learning_spring_security.Service.ServiceStructure.PaymentService;
import com.example.learning_spring_security.ServiceMapper.PaymentMapper;
import com.example.learning_spring_security.dto.Request.PaymentRequest;
import com.example.learning_spring_security.dto.Response.PaymentResponse;

import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Override
    public PaymentResponse processPayment(Long orderId, PaymentRequest request) {
        OrderDetail order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getPayment() != null) {
            throw new BadRequestException("Order already has a payment");
        }

        Payment payment = PaymentMapper.toEntity(request);
        payment.setOrderDetail(order);
        payment.setPaymentDate(LocalDateTime.now());

        // Simulate payment processing (in real app, integrate with payment gateway)
        payment.setStatus("COMPLETED");
        payment.setTransactionId(generateTransactionId());

        Payment savedPayment = paymentRepository.save(payment);

        // Update order status
        order.setStatus("PROCESSING");
        orderRepository.save(order);

        return PaymentMapper.toResponse(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderDetailId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order id: " + orderId));
        return PaymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByTransactionId(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with transaction id: " + transactionId));
        return PaymentMapper.toResponse(payment);
    }

    @Override
    public PaymentResponse updatePaymentStatus(Long paymentId, String status, String transactionId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        payment.setStatus(status);
        if (transactionId != null) {
            payment.setTransactionId(transactionId);
        }

        if ("COMPLETED".equals(status)) {
            payment.setPaymentDate(LocalDateTime.now());
        }

        Payment updatedPayment = paymentRepository.save(payment);
        return PaymentMapper.toResponse(updatedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByOrderDetailUserId(userId)
                .stream()
                .map(PaymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentHistory(Long userId, String status, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return paymentRepository.findPaymentHistory(userId, status, startDate, endDate, pageable)
                .map(PaymentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentDetailByUserId(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findByIdAndOrderDetailUserId(paymentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId + " for user: " + userId));
        return PaymentMapper.toResponse(payment);
    }

    private String generateTransactionId() {
        return "TXN-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
}