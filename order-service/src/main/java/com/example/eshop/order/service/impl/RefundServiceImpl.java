package com.example.eshop.order.service.impl;

import com.example.eshop.common.exception.BusinessLogicException;
import com.example.eshop.order.constant.RefundStatus;
import com.example.eshop.order.exception.InvalidRefundStatusException;
import com.example.eshop.order.exception.RefundNotFoundException;
import com.example.eshop.order.model.Refund;
import com.example.eshop.order.model.RefundStatusHistory;
import com.example.eshop.order.model.Return;
import com.example.eshop.order.repository.RefundRepository;
import com.example.eshop.order.repository.RefundStatusHistoryRepository;
import com.example.eshop.order.service.RefundService;
import com.example.eshop.order.dto.request.CancelRefundRequest;
import com.example.eshop.order.dto.request.GetRefundListRequest;
import com.example.eshop.order.dto.request.ProcessRefundRequest;
import com.example.eshop.order.dto.response.RefundListResponse;
import com.example.eshop.order.dto.response.RefundPageResponse;
import com.example.eshop.order.dto.response.RefundSummaryResponse;
import com.example.eshop.order.dto.response.ResponseErrorTemplate;
import com.example.eshop.order.dto.response.StatusHistoryResponse;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundServiceImpl implements RefundService {

    private static final String REFUND_ID_PREFIX = "RFD";
    private static final String ID_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int ID_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    private final RefundRepository refundRepository;
    private final RefundStatusHistoryRepository refundStatusHistoryRepository;

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
            String[] parts = value.split(",");
            if (parts.length != 2) {
                throw new BusinessLogicException("criteriaValue for type 5 must be 'fromDate,toDate'");
            }
            LocalDateTime fromDate = LocalDateTime.parse(parts[0].trim());
            LocalDateTime toDate = LocalDateTime.parse(parts[1].trim());
            if (fromDate.isAfter(toDate)) {
                throw new BusinessLogicException("fromDate must not be greater than toDate");
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
        refundRepository.findByRefundId(refundId)
                .orElseThrow(() -> new RefundNotFoundException(refundId));

        // Was a call into ProductService (catalog-service) using the sub-category resolved by
        // navigating orderItem -> productSku -> product -> subCategory, all of which now live
        // in catalog-service.
        // TODO: cross-service call via Feign to catalog-service to fetch similar products.
        log.warn("getSimilarProducts for refund {} requires catalog-service integration; returning empty list", refundId);
        return ResponseErrorTemplate.success(
                "Similar products lookup requires catalog-service integration (not yet wired)", List.of());
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

        // Was syncPaymentTransactionIfFullyRefunded — flipped the PaymentTransaction to REFUNDED
        // once fully refunded. payment-service now owns that entity.
        // TODO: cross-service call via Feign to payment-service to notify it a refund was processed.
        log.info("Refund {} processed by {}; payment-service should be notified of the refunded amount", refundId, actor);

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

        // Was validated against a payment-service PaymentTransaction (looking up the SUCCESS
        // transaction for the order and capping the refund at its remaining refundable amount).
        // TODO: cross-service call via Feign to payment-service to find the paid transaction and
        // enforce the remaining-refundable-amount check; for now the return's own amount is trusted.
        BigDecimal amount = returnRequest.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessLogicException("Refund amount must be greater than zero");
        }

        String actor = currentUsername();
        Refund refund = Refund.builder()
                .refundId(generateRefundId())
                .orderId(returnRequest.getOrderId())
                .customerId(returnRequest.getCustomerId())
                .paymentTransactionId(null)
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
