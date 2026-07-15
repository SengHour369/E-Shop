package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Enumeration.PaymentMethod;
import com.example.learning_spring_security.Enumeration.TransactionStatus;
import com.example.learning_spring_security.Exception.ExceptionService.BadRequestException;
import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.OrderDetail;
import com.example.learning_spring_security.Model.Payment;
import com.example.learning_spring_security.Repository.OrderRepository;
import com.example.learning_spring_security.Repository.PaymentRepository;
import com.example.learning_spring_security.Service.ServiceStructure.PaymentService;
import com.example.learning_spring_security.Service.ServiceStructure.PaymentTransactionService;
import com.example.learning_spring_security.ServiceMapper.PaymentMapper;
import com.example.learning_spring_security.dto.Request.GetPaymentRequest;
import com.example.learning_spring_security.dto.Request.PaymentRequest;
import com.example.learning_spring_security.dto.Request.PaymentTransactionRequest;
import com.example.learning_spring_security.dto.Request.PaymentTransactionStatusUpdateRequest;
import com.example.learning_spring_security.dto.Response.PaymentPageResponse;
import com.example.learning_spring_security.dto.Response.PaymentResponse;
import com.example.learning_spring_security.dto.Response.PaymentTransactionResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentTransactionService paymentTransactionService;

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getPayments(GetPaymentRequest request) {
        log.info("getPayments: criteriaType={}, criteriaValue={}, page={}, size={}",
                request.getCriteriaType(), request.getCriteriaValue(), request.getPage(), request.getSize());

        Pageable pageable = PageRequest.of(
                request.getPage() - 1,
                request.getSize(),
                Sort.by("paymentDate").descending()
        );

        Integer type = request.getCriteriaType();
        String value = request.getCriteriaValue();

        Page<Payment> page;
        String successMsg;

        if (type == null || type == 0 || value == null || value.isBlank()) {
            page = paymentRepository.findAll(pageable);
            successMsg = "Retrieved all payments";

        } else if (type == 1) {
            // by userId
            page = paymentRepository.findByOrderDetailUserId(Long.parseLong(value), pageable);
            successMsg = "Retrieved payments by user";

        } else if (type == 2) {
            // by orderId
            page = paymentRepository.findByOrderDetailId(Long.parseLong(value), pageable);
            successMsg = "Retrieved payments by order";

        } else if (type == 3) {
            // by status
            page = paymentRepository.findByStatus(value, pageable);
            successMsg = "Retrieved payments by status";

        } else if (type == 4) {
            // by paymentMethod
            page = paymentRepository.findByPaymentMethod(value, pageable);
            successMsg = "Retrieved payments by payment method";

        } else if (type == 5) {
            // by userId + status, criteriaValue format: "userId:status"
            String[] parts = value.split(":");
            if (parts.length != 2) {
                throw new com.example.learning_spring_security.Exception.ExceptionService.BadRequestException(
                        "criteriaValue for type 5 must be 'userId:status'");
            }
            page = paymentRepository.findPaymentHistory(
                    Long.parseLong(parts[0].trim()),
                    parts[1].trim(),
                    null, null,
                    pageable
            );
            successMsg = "Retrieved payments by user and status";

        } else {
            page = paymentRepository.findAll(pageable);
            successMsg = "Retrieved all payments";
        }

        List<PaymentResponse> payload = page.getContent()
                .stream()
                .map(PaymentMapper::toResponse)
                .toList();

        PaymentPageResponse pageResponse = PaymentPageResponse.builder()
                .payload(payload)
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber() + 1)
                .pageSize(page.getSize())
                .build();

        String message = page.isEmpty() ? "No payments found" : successMsg;
        return ResponseErrorTemplate.success(message, pageResponse);
    }

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

        // Record the money received as a PaymentTransaction so downstream flows
        // (e.g. refunds) can find a SUCCESS transaction for this order.
        recordSuccessfulTransaction(order, savedPayment);

        return PaymentMapper.toResponse(savedPayment);
    }

    private void recordSuccessfulTransaction(OrderDetail order, Payment payment) {
        try {
            PaymentTransactionRequest txnRequest = PaymentTransactionRequest.builder()
                    .orderId(order.getId())
                    .customerId(order.getUser() != null ? order.getUser().getId() : null)
                    .paymentMethod(parsePaymentMethod(payment.getPaymentMethod()))
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency() != null ? payment.getCurrency() : "USD")
                    .remarks("Auto-created on payment success for order " + order.getId())
                    .build();

            PaymentTransactionResponse txn = paymentTransactionService.createTransaction(txnRequest);

            paymentTransactionService.updateTransactionStatus(txn.getId(),
                    PaymentTransactionStatusUpdateRequest.builder()
                            .newStatus(TransactionStatus.SUCCESS)
                            .changedBy("SYSTEM")
                            .reason("Payment completed")
                            .build());

            log.info("PaymentTransaction {} recorded as SUCCESS for order {}",
                    txn.getTransactionNo(), order.getId());
        } catch (Exception e) {
            // Re-throw so the whole @Transactional processPayment rolls back: we never want a
            // COMPLETED payment without its matching PaymentTransaction (refunds depend on it).
            log.error("Failed to record PaymentTransaction for order {}: {}", order.getId(), e.getMessage(), e);
            throw e;
        }
    }

    private PaymentMethod parsePaymentMethod(String method) {
        if (method == null) return null;
        try {
            return PaymentMethod.valueOf(method.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown payment method '{}' for PaymentTransaction; storing null", method);
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        return ResponseErrorTemplate.success("Payment retrieved successfully", PaymentMapper.toResponse(payment));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getPaymentsByUser(Long userId) {
        List<PaymentResponse> payments = paymentRepository.findByOrderDetailUserId(userId)
                .stream()
                .map(PaymentMapper::toResponse)
                .toList();
        return ResponseErrorTemplate.success("Payments retrieved successfully", payments);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getPaymentDetailByUser(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findByIdAndOrderDetailUserId(paymentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with id: " + paymentId + " for user: " + userId));
        return ResponseErrorTemplate.success("Payment detail retrieved successfully", PaymentMapper.toResponse(payment));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getPaymentByOrder(Long orderId) {
        Payment payment = paymentRepository.findByOrderDetailId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order id: " + orderId));
        return ResponseErrorTemplate.success("Payment retrieved successfully", PaymentMapper.toResponse(payment));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getPaymentByTransaction(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with transaction id: " + transactionId));
        return ResponseErrorTemplate.success("Payment retrieved successfully", PaymentMapper.toResponse(payment));
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