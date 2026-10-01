package com.example.eshop.payment.service.impl;

import com.example.eshop.common.exception.BusinessLogicException;
import com.example.eshop.common.exception.ResourceNotFoundException;
import com.example.eshop.payment.enumeration.PaymentMethod;
import com.example.eshop.payment.enumeration.TransactionStatus;
import com.example.eshop.payment.model.Payment;
import com.example.eshop.payment.repository.PaymentRepository;
import com.example.eshop.payment.service.PaymentService;
import com.example.eshop.payment.service.PaymentTransactionService;
import com.example.eshop.payment.mapper.PaymentMapper;
import com.example.eshop.payment.dto.request.GetPaymentRequest;
import com.example.eshop.payment.dto.request.PaymentRequest;
import com.example.eshop.payment.dto.request.PaymentTransactionRequest;
import com.example.eshop.payment.dto.response.PaymentPageResponse;
import com.example.eshop.payment.dto.response.PaymentResponse;
import com.example.eshop.payment.dto.response.ResponseErrorTemplate;
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
            page = paymentRepository.findByUserId(Long.parseLong(value), pageable);
            successMsg = "Retrieved payments by user";

        } else if (type == 2) {
            // by orderId
            page = paymentRepository.findByOrderId(Long.parseLong(value), pageable);
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
                throw new BusinessLogicException("criteriaValue for type 5 must be 'userId:status'");
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
        // Cross-service: the monolith looked up the Order (order-service) here, guarded
        // against a duplicate payment via order.getPayment() != null, and flipped the
        // order's status to PROCESSING after a successful payment.
        // TODO: cross-service call via Feign to order-service to (a) validate the order
        // exists, (b) confirm it has no payment yet, and (c) update its status once this
        // payment completes.
        if (paymentRepository.findByOrderId(orderId).isPresent()) {
            throw new BusinessLogicException("Order already has a payment");
        }

        Payment payment = PaymentMapper.toEntity(request);
        payment.setOrderId(orderId);
        payment.setPaymentDate(LocalDateTime.now());

        // Simulate payment processing (in real app, integrate with payment gateway)
        payment.setStatus("COMPLETED");
        payment.setTransactionId(generateTransactionId());

        Payment savedPayment = paymentRepository.save(payment);

        // Record the money received as a PaymentTransaction so downstream flows
        // (e.g. refunds) can find a SUCCESS transaction for this order.
        recordSuccessfulTransaction(orderId, savedPayment);

        return PaymentMapper.toResponse(savedPayment);
    }

    private void recordSuccessfulTransaction(Long orderId, Payment payment) {
        try {
            PaymentTransactionRequest txnRequest = PaymentTransactionRequest.builder()
                    .orderId(orderId)
                    .customerId(payment.getUserId())
                    .paymentMethod(PaymentMethod.fromString(payment.getPaymentMethod()))
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency() != null ? payment.getCurrency() : "USD")
                    .remarks("Auto-created on payment success for order " + orderId)
                    .build();

            paymentTransactionService.recordTransaction(
                    txnRequest, TransactionStatus.SUCCESS, "SYSTEM", "Payment completed");
        } catch (Exception e) {
            // Re-throw so the whole @Transactional processPayment rolls back: we never want a
            // COMPLETED payment without its matching PaymentTransaction (refunds depend on it).
            log.error("Failed to record PaymentTransaction for order {}: {}", orderId, e.getMessage(), e);
            throw e;
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
        List<PaymentResponse> payments = paymentRepository.findByUserId(userId)
                .stream()
                .map(PaymentMapper::toResponse)
                .toList();
        return ResponseErrorTemplate.success("Payments retrieved successfully", payments);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getPaymentDetailByUser(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with id: " + paymentId + " for user: " + userId));
        return ResponseErrorTemplate.success("Payment detail retrieved successfully", PaymentMapper.toResponse(payment));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getPaymentByOrder(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
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
        Payment payment = paymentRepository.findByOrderId(orderId)
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
        return paymentRepository.findByUserId(userId)
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
        Payment payment = paymentRepository.findByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId + " for user: " + userId));
        return PaymentMapper.toResponse(payment);
    }

    private String generateTransactionId() {
        return "TXN-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
}
