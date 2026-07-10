package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Enumeration.TransactionStatus;
import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;

import com.example.learning_spring_security.Model.PaymentTransaction;
import com.example.learning_spring_security.Model.PaymentTransactionStatusHistory;
import com.example.learning_spring_security.Repository.PaymentTransactionRepository;
import com.example.learning_spring_security.Repository.PaymentTransactionStatusHistoryRepository;
import com.example.learning_spring_security.Service.ServiceStructure.PaymentTransactionService;
import com.example.learning_spring_security.ServiceMapper.PaymentTransactionMapper;
import com.example.learning_spring_security.dto.Request.GetPaymentTransactionRequest;
import com.example.learning_spring_security.dto.Request.PaymentTransactionRequest;
import com.example.learning_spring_security.dto.Request.PaymentTransactionStatusUpdateRequest;
import com.example.learning_spring_security.dto.Response.PaymentTransactionPageResponse;
import com.example.learning_spring_security.dto.Response.PaymentTransactionResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentTransactionServiceImpl implements PaymentTransactionService {

    private static final String TRANSACTION_NO_PREFIX = "PAY";
    private static final String TRANSACTION_NO_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int TRANSACTION_NO_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentTransactionStatusHistoryRepository statusHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getTransactions(GetPaymentTransactionRequest request) {
        log.info("getTransactions: criteriaType={}, criteriaValue={}, page={}, size={}",
                request.getCriteriaType(), request.getCriteriaValue(), request.getPage(), request.getSize());

        Pageable pageable = PageRequest.of(
                request.getPage() - 1,
                request.getSize(),
                Sort.by("id").descending()
        );

        Integer type = request.getCriteriaType();
        String value = request.getCriteriaValue();

        Page<PaymentTransaction> page;
        String successMsg;

        if (type == null || type == 0 || value == null || value.isBlank()) {
            page = paymentTransactionRepository.findAll(pageable);
            successMsg = "Retrieved all payment transactions";

        } else if (type == 1) {
            // by orderId
            page = paymentTransactionRepository.findByOrder(Long.parseLong(value), pageable);
            successMsg = "Retrieved payment transactions by order";

        } else if (type == 2) {
            // by customerId
            page = paymentTransactionRepository.findByCustomer(Long.parseLong(value), pageable);
            successMsg = "Retrieved payment transactions by customer";

        } else if (type == 3) {
            // by status
            page = paymentTransactionRepository.findByStatus(TransactionStatus.valueOf(value), pageable);
            successMsg = "Retrieved payment transactions by status";

        } else {
            page = paymentTransactionRepository.findAll(pageable);
            successMsg = "Retrieved all payment transactions";
        }

        List<PaymentTransactionResponse> payload = page.getContent()
                .stream()
                .map(PaymentTransactionMapper::toResponse)
                .toList();

        PaymentTransactionPageResponse pageResponse = PaymentTransactionPageResponse.builder()
                .payload(payload)
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber() + 1)
                .pageSize(page.getSize())
                .build();

        String message = page.isEmpty() ? "No payment transactions found" : successMsg;
        return ResponseErrorTemplate.success(message, pageResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getTransactionById(Long id) {
        PaymentTransaction transaction = paymentTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found with id: " + id));
        List<PaymentTransactionStatusHistory> history =
                statusHistoryRepository.findByTransactionOrderByChangedAtDesc(transaction.getId());
        return ResponseErrorTemplate.success("Payment transaction retrieved successfully",
                PaymentTransactionMapper.toResponse(transaction, history));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getTransactionByNo(String transactionNo) {
        PaymentTransaction transaction = paymentTransactionRepository.findByTransactionNo(transactionNo)
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found with no: " + transactionNo));
        List<PaymentTransactionStatusHistory> history =
                statusHistoryRepository.findByTransactionOrderByChangedAtDesc(transaction.getId());
        return ResponseErrorTemplate.success("Payment transaction retrieved successfully",
                PaymentTransactionMapper.toResponse(transaction, history));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getTransactionsByOrder(Long orderId) {
        List<PaymentTransactionResponse> transactions = paymentTransactionRepository.findByOrder(orderId)
                .stream()
                .map(PaymentTransactionMapper::toResponse)
                .toList();
        return ResponseErrorTemplate.success("Payment transactions retrieved successfully", transactions);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getTransactionsByCustomer(Long customerId) {
        List<PaymentTransactionResponse> transactions = paymentTransactionRepository.findByCustomer(customerId)
                .stream()
                .map(PaymentTransactionMapper::toResponse)
                .toList();
        return ResponseErrorTemplate.success("Payment transactions retrieved successfully", transactions);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getTransactionStatusHistory(Long transactionId) {
        if (!paymentTransactionRepository.existsById(transactionId)) {
            throw new ResourceNotFoundException("Payment transaction not found with id: " + transactionId);
        }
        List<PaymentTransactionStatusHistory> history =
                statusHistoryRepository.findByTransactionOrderByChangedAtDesc(transactionId);
        return ResponseErrorTemplate.success("Payment transaction status history retrieved successfully",
                history.stream().map(PaymentTransactionMapper::toHistoryResponse).toList());
    }

    @Override
    public PaymentTransactionResponse createTransaction(PaymentTransactionRequest request) {
        PaymentTransaction transaction = PaymentTransactionMapper.toEntity(request, generateTransactionNo());
        PaymentTransaction saved = paymentTransactionRepository.save(transaction);

        PaymentTransactionStatusHistory initialHistory = PaymentTransactionStatusHistory.builder()
                .transaction(saved.getId())
                .oldStatus(null)
                .newStatus(saved.getStatus())
                .changedAt(LocalDateTime.now())
                .changedBy("SYSTEM")
                .reason("Transaction created")
                .build();
        statusHistoryRepository.save(initialHistory);

        return PaymentTransactionMapper.toResponse(saved);
    }

    @Override
    public PaymentTransactionResponse updateTransactionStatus(Long transactionId, PaymentTransactionStatusUpdateRequest request) {
        PaymentTransaction transaction = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found with id: " + transactionId));

        TransactionStatus oldStatus = transaction.getStatus();
        transaction.setStatus(request.getNewStatus());
        PaymentTransaction updated = paymentTransactionRepository.save(transaction);

        PaymentTransactionStatusHistory history = PaymentTransactionStatusHistory.builder()
                .transaction(updated.getId())
                .oldStatus(oldStatus)
                .newStatus(request.getNewStatus())
                .changedAt(LocalDateTime.now())
                .changedBy(request.getChangedBy())
                .reason(request.getReason())
                .build();
        statusHistoryRepository.save(history);

        return PaymentTransactionMapper.toResponse(updated);
    }

    private String generateTransactionNo() {
        String transactionNo;
        do {
            transactionNo = TRANSACTION_NO_PREFIX + "-" + generateRandomPart();
        } while (paymentTransactionRepository.existsByTransactionNo(transactionNo));
        return transactionNo;
    }

    private String generateRandomPart() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < TRANSACTION_NO_LENGTH; i++) {
            int index = random.nextInt(TRANSACTION_NO_CHARACTERS.length());
            builder.append(TRANSACTION_NO_CHARACTERS.charAt(index));
        }
        return builder.toString();
    }
}