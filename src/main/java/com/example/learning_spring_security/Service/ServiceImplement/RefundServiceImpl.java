package com.example.learning_spring_security.Service.ServiceImplement;


import com.example.learning_spring_security.Constant.RefundStatus;
import com.example.learning_spring_security.Enumeration.TransactionStatus;
import com.example.learning_spring_security.Exception.ExceptionService.BadRequestException;
import com.example.learning_spring_security.Exception.ExceptionService.InvalidRefundStatusException;
import com.example.learning_spring_security.Exception.ExceptionService.PaymentNotFoundException;
import com.example.learning_spring_security.Exception.ExceptionService.RefundAmountExceededException;
import com.example.learning_spring_security.Exception.ExceptionService.RefundNotFoundException;
import com.example.learning_spring_security.Model.OrderItem;
import com.example.learning_spring_security.Model.PaymentTransaction;
import com.example.learning_spring_security.Model.Refund;
import com.example.learning_spring_security.Model.RefundStatusHistory;
import com.example.learning_spring_security.Model.Return;
import com.example.learning_spring_security.Repository.OrderItemRepository;
import com.example.learning_spring_security.Repository.PaymentTransactionRepository;
import com.example.learning_spring_security.Repository.RefundRepository;
import com.example.learning_spring_security.Repository.RefundStatusHistoryRepository;
import com.example.learning_spring_security.Service.ServiceStructure.ProductService;
import com.example.learning_spring_security.Service.ServiceStructure.RefundService;
import com.example.learning_spring_security.dto.Request.CancelRefundRequest;
import com.example.learning_spring_security.dto.Request.GetProductRequest;
import com.example.learning_spring_security.dto.Request.GetRefundListRequest;
import com.example.learning_spring_security.dto.Request.ProcessRefundRequest;
import com.example.learning_spring_security.dto.Response.RefundListResponse;
import com.example.learning_spring_security.dto.Response.RefundPageResponse;
import com.example.learning_spring_security.dto.Response.RefundSummaryResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import com.example.learning_spring_security.dto.Response.StatusHistoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundServiceImpl implements RefundService {

    private static final String REFUND_ID_PREFIX = "RFD";
    private static final String ID_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int ID_LENGTH = 8;
    private static final int SIMILAR_PRODUCTS_CRITERIA_TYPE = 2; // matches ProductServiceImpl#getProducts "by subCategoryId"

    private final SecureRandom random = new SecureRandom();

    private final RefundRepository refundRepository;
    private final RefundStatusHistoryRepository refundStatusHistoryRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductService productService;

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getRefundSummary() {
        RefundSummaryResponse summary = refundRepository.getRefundSummary();
        if (summary.getTotalRefunds() == null) summary.setTotalRefunds(0L);
        if (summary.getCompletedRefunds() == null) summary.setCompletedRefunds(0L);
        if (summary.getPendingRefunds() == null) summary.setPendingRefunds(0L);
        if (summary.getRefundedAmount() == null) summary.setRefundedAmount(BigDecimal.ZERO);

        return ResponseErrorTemplate.success("Refund summary retrieved successfully", summary);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getRefundList(GetRefundListRequest request) {
        log.info("getRefundList: criteriaType={}, criteriaValue={}, page={}, size={}",
                request.getCriteriaType(), request.getCriteriaValue(), request.getPage(), request.getSize());

        int page = request.getPage() < 1 ? 1 : request.getPage();
        int size = request.getSize() < 1 ? 10 : request.getSize();
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("requestedAt").descending());

        Integer type = request.getCriteriaType();
        String value = request.getCriteriaValue();

        Page<RefundListResponse> result;
        String successMsg;

        if (type == null || type == 0 || value == null || value.isBlank()) {
            result = refundRepository.findAllRefunds(pageable);
            successMsg = "Retrieved all refunds";

        } else if (type == 1) {
            result = refundRepository.findByRefundIdForList(value.trim(), pageable);
            successMsg = "Retrieved refunds by refund ID";

        } else if (type == 2) {
            result = refundRepository.findByOrderNo(value.trim(), pageable);
            successMsg = "Retrieved refunds by order number";

        } else if (type == 3) {
            result = refundRepository.findByCustomerNameContaining(value.trim(), pageable);
            successMsg = "Retrieved refunds by customer name";

        } else if (type == 4) {
            result = refundRepository.findByStatus(value.trim(), pageable);
            successMsg = "Retrieved refunds by status";

        } else if (type == 5) {
            // by date range, criteriaValue format: "fromDate,toDate" (yyyy-MM-ddTHH:mm:ss)
            String[] parts = value.split(",");
            if (parts.length != 2) {
                throw new BadRequestException("criteriaValue for type 5 must be 'fromDate,toDate'");
            }
            LocalDateTime fromDate = LocalDateTime.parse(parts[0].trim());
            LocalDateTime toDate = LocalDateTime.parse(parts[1].trim());
            if (fromDate.isAfter(toDate)) {
                throw new BadRequestException("fromDate must not be greater than toDate");
            }
            result = refundRepository.findByRequestedAtBetween(fromDate, toDate, pageable);
            successMsg = "Retrieved refunds by date range";

        } else {
            result = refundRepository.findAllRefunds(pageable);
            successMsg = "Retrieved all refunds";
        }

        RefundPageResponse pageResponse = RefundPageResponse.builder()
                .payload(result.getContent())
                .totalItems(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .currentPage(result.getNumber() + 1)
                .pageSize(result.getSize())
                .build();

        String message = result.isEmpty() ? "No refunds found" : successMsg;
        return ResponseErrorTemplate.success(message, pageResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getRefundDetail(String refundId) {
        var detail = refundRepository.findDetailByRefundId(refundId)
                .orElseThrow(() -> new RefundNotFoundException(refundId));
        return ResponseErrorTemplate.success("Refund detail retrieved successfully", detail);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getRefundHistory(String refundId) {
        Refund refund = refundRepository.findByRefundId(refundId)
                .orElseThrow(() -> new RefundNotFoundException(refundId));

        List<StatusHistoryResponse> history = refundStatusHistoryRepository
                .findByRefundOrderByChangedAtDesc(refund.getId())
                .stream()
                .map(h -> StatusHistoryResponse.builder()
                        .oldStatus(h.getOldStatus())
                        .newStatus(h.getNewStatus())
                        .changedAt(h.getChangedAt())
                        .changedBy(h.getChangedBy())
                        .remark(h.getRemark())
                        .build())
                .toList();

        return ResponseErrorTemplate.success("Refund history retrieved successfully", history);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getSimilarProducts(String refundId, Integer page, Integer size) {
        Refund refund = refundRepository.findByRefundId(refundId)
                .orElseThrow(() -> new RefundNotFoundException(refundId));

        List<OrderItem> orderItems = orderItemRepository.findByOrderDetailId(refund.getOrderId());

        Long subCategoryId = orderItems.stream()
                .map(item -> item.getProductSku().getProduct().getSubCategory())
                .filter(Objects::nonNull)
                .map(subCategory -> subCategory.getId())
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No sub-category found for refund " + refundId));

        GetProductRequest request = GetProductRequest.builder()
                .criteriaType(SIMILAR_PRODUCTS_CRITERIA_TYPE)
                .criteriaValue(String.valueOf(subCategoryId))
                .page(page == null || page < 1 ? 1 : page)
                .size(size == null || size < 1 ? 10 : size)
                .build();

        return productService.getProducts(request);
    }

    @Override
    @Transactional
    public ResponseErrorTemplate processRefund(String refundId, ProcessRefundRequest request) {
        Refund refund = refundRepository.findByRefundIdForUpdate(refundId)
                .orElseThrow(() -> new RefundNotFoundException(refundId));

        if (!RefundStatus.PENDING.equals(refund.getStatus())) {
            throw new InvalidRefundStatusException(refundId, refund.getStatus());
        }

        String actor = currentUsername();
        String oldStatus = refund.getStatus();
        refund.setStatus(RefundStatus.PROCESSED);
        refund.setProcessedAt(LocalDateTime.now());
        refund.setProcessedBy(actor);
        refund.setUpdatedBy(actor);
        if (request != null && request.getRemark() != null) {
            refund.setRemark(request.getRemark());
        }

        refundRepository.save(refund);
        recordHistory(refund.getId(), oldStatus, RefundStatus.PROCESSED, actor, refund.getRemark());
        syncPaymentTransactionIfFullyRefunded(refund);

        log.info("Refund {} processed by {}", refundId, actor);
        return getRefundDetail(refundId);
    }

    @Override
    @Transactional
    public ResponseErrorTemplate cancelRefund(String refundId, CancelRefundRequest request) {
        Refund refund = refundRepository.findByRefundIdForUpdate(refundId)
                .orElseThrow(() -> new RefundNotFoundException(refundId));

        if (!RefundStatus.PENDING.equals(refund.getStatus())) {
            throw new InvalidRefundStatusException(refundId, refund.getStatus());
        }

        String actor = currentUsername();
        String oldStatus = refund.getStatus();
        refund.setStatus(RefundStatus.CANCELLED);
        refund.setUpdatedBy(actor);
        if (request != null && request.getRemark() != null) {
            refund.setRemark(request.getRemark());
        }

        refundRepository.save(refund);
        recordHistory(refund.getId(), oldStatus, RefundStatus.CANCELLED, actor, refund.getRemark());

        log.info("Refund {} cancelled by {}", refundId, actor);
        return getRefundDetail(refundId);
    }

    @Override
    @Transactional
    public void createRefundFromReturn(Return returnRequest) {
        if (refundRepository.existsByReturnId(returnRequest.getReturnId())) {
            log.info("Refund already exists for return {}, skipping creation", returnRequest.getReturnId());
            return;
        }

        PaymentTransaction paymentTransaction = paymentTransactionRepository.findByOrder(returnRequest.getOrderId())
                .stream()
                .filter(pt -> pt.getStatus() == TransactionStatus.SUCCESS)
                .findFirst()
                .orElseThrow(() -> new PaymentNotFoundException(returnRequest.getOrderId()));

        BigDecimal alreadyRefunded = refundRepository.sumProcessedAmountByPaymentTransactionId(paymentTransaction.getId());
        if (alreadyRefunded == null) alreadyRefunded = BigDecimal.ZERO;
        BigDecimal remaining = paymentTransaction.getAmount().subtract(alreadyRefunded);

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Order " + returnRequest.getOrderId() + " has already been fully refunded");
        }

        BigDecimal amount = returnRequest.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Refund amount must be greater than zero");
        }
        if (amount.compareTo(remaining) > 0) {
            throw new RefundAmountExceededException(amount, remaining);
        }

        String actor = currentUsername();
        Refund refund = Refund.builder()
                .refundId(generateRefundId())
                .orderId(returnRequest.getOrderId())
                .customerId(returnRequest.getCustomerId())
                .paymentTransactionId(paymentTransaction.getId())
                .source("RETURN")
                .returnId(returnRequest.getReturnId())
                .amount(amount)
                .status(RefundStatus.PENDING)
                .reason(returnRequest.getReason())
                .requestedAt(LocalDateTime.now())
                .requestedBy(actor)
                .createdBy(actor)
                .build();

        Refund saved = refundRepository.save(refund);
        recordHistory(saved.getId(), null, RefundStatus.PENDING, actor,
                "Auto-created from approved return " + returnRequest.getReturnId());

        log.info("Refund {} created from return {}", saved.getRefundId(), returnRequest.getReturnId());
    }

    private void syncPaymentTransactionIfFullyRefunded(Refund refund) {
        if (refund.getPaymentTransactionId() == null) return;

        paymentTransactionRepository.findById(refund.getPaymentTransactionId()).ifPresent(pt -> {
            BigDecimal totalProcessed = refundRepository.sumProcessedAmountByPaymentTransactionId(pt.getId());
            if (totalProcessed != null
                    && totalProcessed.compareTo(pt.getAmount()) >= 0
                    && pt.getStatus() != TransactionStatus.REFUNDED) {
                pt.setStatus(TransactionStatus.REFUNDED);
                paymentTransactionRepository.save(pt);
            }
        });
    }

    private void recordHistory(Long refundId, String oldStatus, String newStatus, String changedBy, String remark) {
        RefundStatusHistory history = RefundStatusHistory.builder()
                .refund(refundId)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedAt(LocalDateTime.now())
                .changedBy(changedBy)
                .remark(remark)
                .build();
        refundStatusHistoryRepository.save(history);
    }

    private String generateRefundId() {
        String refundId;
        do {
            refundId = REFUND_ID_PREFIX + "-" + generateRandomPart();
        } while (refundRepository.existsByRefundId(refundId));
        return refundId;
    }

    private String generateRandomPart() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ID_LENGTH; i++) {
            builder.append(ID_CHARACTERS.charAt(random.nextInt(ID_CHARACTERS.length())));
        }
        return builder.toString();
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }
}