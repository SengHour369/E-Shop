package com.example.eshop.order.service.impl;

import com.example.eshop.common.exception.BusinessLogicException;
import com.example.eshop.common.exception.ResourceNotFoundException;
import com.example.eshop.order.constant.ReturnStatus;
import com.example.eshop.order.constant.ReturnType;
import com.example.eshop.order.exception.InvalidReturnStatusException;
import com.example.eshop.order.exception.ReturnNotFoundException;
import com.example.eshop.order.model.OrderItem;
import com.example.eshop.order.model.Return;
import com.example.eshop.order.model.ReturnStatusHistory;
import com.example.eshop.order.repository.OrderRepository;
import com.example.eshop.order.repository.ReturnRequestRepository;
import com.example.eshop.order.repository.ReturnStatusHistoryRepository;
import com.example.eshop.order.service.RefundService;
import com.example.eshop.order.service.ReturnService;
import com.example.eshop.order.dto.request.*;
import com.example.eshop.order.dto.response.ResponseErrorTemplate;
import com.example.eshop.order.dto.response.ReturnListResponse;
import com.example.eshop.order.dto.response.ReturnPageResponse;
import com.example.eshop.order.dto.response.ReturnSummaryResponse;
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

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnServiceImpl implements ReturnService {

    private static final String RETURN_ID_PREFIX = "RET";
    private static final String ID_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int ID_LENGTH = 8;
    private static final Set<String> VALID_RETURN_TYPES =
            Set.of(ReturnType.RETURN, ReturnType.REFUND, ReturnType.EXCHANGE);

    private final SecureRandom random = new SecureRandom();

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final ReturnStatusHistoryRepository returnStatusHistoryRepository;
    private final RefundService refundService;

    @Override
    @Transactional
    public ResponseErrorTemplate createReturn(CreateReturnRequest request) {
        orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.getOrderId()));

        String returnType = request.getReturnType() == null ? null : request.getReturnType().trim().toUpperCase();
        if (!VALID_RETURN_TYPES.contains(returnType)) {
            throw new BusinessLogicException("Invalid return type: " + request.getReturnType()
                    + ". Allowed values: " + VALID_RETURN_TYPES);
        }

        String actor = currentUsername();
        Return returnRequest = Return.builder()
                .returnId(generateReturnId())
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .productId(request.getProductId())
                .returnType(returnType)
                .reason(request.getReason())
                .amount(request.getAmount())
                .status(ReturnStatus.REQUESTED)
                .requestedAt(LocalDateTime.now())
                .requestedBy(actor)
                .createdBy(actor)
                .build();

        Return saved = returnRequestRepository.save(returnRequest);
        recordHistory(saved.getId(), null, ReturnStatus.REQUESTED, actor, saved.getReason());
        log.info("Return {} created for order {} by {}", saved.getReturnId(), saved.getOrderId(), actor);

        return getReturnDetail(saved.getReturnId());
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getReturnSummary() {
        ReturnSummaryResponse summary = returnRequestRepository.getReturnSummary();

        long totalOrders = orderRepository.count();
        long totalReturns = summary.getTotalReturns() != null ? summary.getTotalReturns() : 0L;
        double returnRate = totalOrders == 0 ? 0.0 : Math.round(totalReturns * 1000.0 / totalOrders) / 10.0;
        summary.setReturnRate(returnRate);

        return ResponseErrorTemplate.success("Return summary retrieved successfully", summary);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getReturnDetail(String returnId) {
        var detail = returnRequestRepository.findDetailByReturnId(returnId)
                .orElseThrow(() -> new ReturnNotFoundException(returnId));
        return ResponseErrorTemplate.success("Return detail retrieved successfully", detail);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getReturnHistory(String returnId) {
        Return returnRequest = returnRequestRepository.findByReturnId(returnId)
                .orElseThrow(() -> new ReturnNotFoundException(returnId));

        List<StatusHistoryResponse> history = returnStatusHistoryRepository
                .findByReturnRequestOrderByChangedAtDesc(returnRequest.getId())
                .stream()
                .map(h -> StatusHistoryResponse.builder()
                        .oldStatus(h.getOldStatus())
                        .newStatus(h.getNewStatus())
                        .changedAt(h.getChangedAt())
                        .changedBy(h.getChangedBy())
                        .remark(h.getRemark())
                        .build())
                .toList();

        return ResponseErrorTemplate.success("Return history retrieved successfully", history);
    }

    @Override
    @Transactional
    public ResponseErrorTemplate approveReturn(String returnId, ApproveReturnRequest request) {
        Return returnRequest = returnRequestRepository.findByReturnIdForUpdate(returnId)
                .orElseThrow(() -> new ReturnNotFoundException(returnId));

        if (!ReturnStatus.REQUESTED.equals(returnRequest.getStatus())) {
            throw new InvalidReturnStatusException(returnId, returnRequest.getStatus());
        }

        String actor = currentUsername();
        String oldStatus = returnRequest.getStatus();
        returnRequest.setStatus(ReturnStatus.APPROVED);
        returnRequest.setApprovedAt(LocalDateTime.now());
        returnRequest.setApprovedBy(actor);
        returnRequest.setUpdatedBy(actor);
        if (request != null && request.getRemark() != null) {
            returnRequest.setRemark(request.getRemark());
        }

        returnRequestRepository.save(returnRequest);
        recordHistory(returnRequest.getId(), oldStatus, ReturnStatus.APPROVED, actor, returnRequest.getRemark());
        log.info("Return {} approved by {}", returnId, actor);

        return getReturnDetail(returnId);
    }

    @Override
    @Transactional
    public ResponseErrorTemplate rejectReturn(String returnId, RejectReturnRequest request) {
        Return returnRequest = returnRequestRepository.findByReturnIdForUpdate(returnId)
                .orElseThrow(() -> new ReturnNotFoundException(returnId));

        if (!ReturnStatus.REQUESTED.equals(returnRequest.getStatus())) {
            throw new InvalidReturnStatusException(returnId, returnRequest.getStatus());
        }

        String actor = currentUsername();
        String oldStatus = returnRequest.getStatus();
        returnRequest.setStatus(ReturnStatus.REJECTED);
        returnRequest.setRejectedAt(LocalDateTime.now());
        returnRequest.setRejectedBy(actor);
        returnRequest.setUpdatedBy(actor);
        if (request != null && request.getRemark() != null) {
            returnRequest.setRemark(request.getRemark());
        }

        returnRequestRepository.save(returnRequest);
        recordHistory(returnRequest.getId(), oldStatus, ReturnStatus.REJECTED, actor, returnRequest.getRemark());
        log.info("Return {} rejected by {}", returnId, actor);

        return getReturnDetail(returnId);
    }

    @Override
    @Transactional
    public ResponseErrorTemplate receiveReturn(String returnId, ReceiveReturnRequest request) {
        Return returnRequest = returnRequestRepository.findByReturnIdForUpdate(returnId)
                .orElseThrow(() -> new ReturnNotFoundException(returnId));

        if (!ReturnStatus.APPROVED.equals(returnRequest.getStatus())) {
            throw new InvalidReturnStatusException(returnId, returnRequest.getStatus());
        }

        String actor = currentUsername();
        String oldStatus = returnRequest.getStatus();
        returnRequest.setStatus(ReturnStatus.RECEIVED);
        returnRequest.setReceivedAt(LocalDateTime.now());
        returnRequest.setReceivedBy(actor);
        returnRequest.setUpdatedBy(actor);
        if (request != null && request.getRemark() != null) {
            returnRequest.setRemark(request.getRemark());
        }

        returnRequestRepository.save(returnRequest);
        recordHistory(returnRequest.getId(), oldStatus, ReturnStatus.RECEIVED, actor, returnRequest.getRemark());
        log.info("Return {} marked received by {}", returnId, actor);

        return getReturnDetail(returnId);
    }

    @Override
    @Transactional
    public ResponseErrorTemplate startInspection(String returnId) {
        Return returnRequest = returnRequestRepository.findByReturnIdForUpdate(returnId)
                .orElseThrow(() -> new ReturnNotFoundException(returnId));

        if (!ReturnStatus.RECEIVED.equals(returnRequest.getStatus())) {
            throw new InvalidReturnStatusException(returnId, returnRequest.getStatus());
        }

        String actor = currentUsername();
        String oldStatus = returnRequest.getStatus();
        returnRequest.setStatus(ReturnStatus.INSPECTING);
        returnRequest.setUpdatedBy(actor);

        returnRequestRepository.save(returnRequest);
        recordHistory(returnRequest.getId(), oldStatus, ReturnStatus.INSPECTING, actor, null);
        log.info("Return {} inspection started by {}", returnId, actor);

        return getReturnDetail(returnId);
    }

    @Override
    @Transactional
    public ResponseErrorTemplate completeInspection(String returnId, CompleteInspectionRequest request) {
        Return returnRequest = returnRequestRepository.findByReturnIdForUpdate(returnId)
                .orElseThrow(() -> new ReturnNotFoundException(returnId));

        if (!ReturnStatus.INSPECTING.equals(returnRequest.getStatus())) {
            throw new InvalidReturnStatusException(returnId, returnRequest.getStatus());
        }

        boolean passed = request != null && request.isPassed();
        String remark = request != null ? request.getRemark() : null;
        String actor = currentUsername();
        String oldStatus = returnRequest.getStatus();

        returnRequest.setInspectedAt(LocalDateTime.now());
        returnRequest.setInspectedBy(actor);
        returnRequest.setUpdatedBy(actor);
        if (remark != null) {
            returnRequest.setRemark(remark);
        }

        if (passed) {
            returnRequest.setStatus(ReturnStatus.COMPLETED);
            returnRequest.setCompletedAt(LocalDateTime.now());
            returnRequest.setCompletedBy(actor);
        } else {
            returnRequest.setStatus(ReturnStatus.REJECTED);
            returnRequest.setRejectedAt(LocalDateTime.now());
            returnRequest.setRejectedBy(actor);
        }

        returnRequestRepository.save(returnRequest);
        recordHistory(returnRequest.getId(), oldStatus, returnRequest.getStatus(), actor, remark);
        log.info("Return {} inspection completed by {}, passed={}", returnId, actor, passed);

        if (passed) {
            logInventoryRestockNeeded(returnRequest);

            if (!ReturnType.EXCHANGE.equals(returnRequest.getReturnType())) {
                refundService.createRefundFromReturn(returnRequest);
            } else {
                log.warn("Return {} is an EXCHANGE — replacement order/shipment creation is not yet implemented", returnId);
            }
        }

        return getReturnDetail(returnId);
    }

    /**
     * Was increaseInventoryForReturnedProduct — resolved the returned quantity from the local
     * order item and pushed a stock increase directly into InventoryRepository (catalog-service).
     * TODO: cross-service call via Feign to catalog-service to restock inventory for the returned
     * product once that client exists; for now this only logs the quantity that needs restocking.
     */
    private void logInventoryRestockNeeded(Return returnRequest) {
        Long returnedQuantity = orderRepository.findByIdWithItems(returnRequest.getOrderId())
                .flatMap(order -> order.getOrderItems().stream()
                        .filter(item -> item.getProductSkuId() != null)
                        .map(OrderItem::getQuantity)
                        .findFirst())
                .orElse(1L);

        log.info("Return {} passed inspection — catalog-service should restock productId={} by quantity={}",
                returnRequest.getReturnId(), returnRequest.getProductId(), returnedQuantity);
    }

    private void recordHistory(Long returnId, String oldStatus, String newStatus, String changedBy, String remark) {
        ReturnStatusHistory history = ReturnStatusHistory.builder()
                .returnRequest(returnId)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedAt(LocalDateTime.now())
                .changedBy(changedBy)
                .remark(remark)
                .build();
        returnStatusHistoryRepository.save(history);
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }

    private String generateReturnId() {
        String returnId;
        do {
            returnId = RETURN_ID_PREFIX + "-" + generateRandomPart();
        } while (returnRequestRepository.existsByReturnId(returnId));
        return returnId;
    }

    private String generateRandomPart() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ID_LENGTH; i++) {
            builder.append(ID_CHARACTERS.charAt(random.nextInt(ID_CHARACTERS.length())));
        }
        return builder.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getReturns(GetReturnRequest request) {
        int page = request.getPage() < 1 ? 1 : request.getPage();
        int size = request.getSize() < 1 ? 10 : request.getSize();
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("requestedAt").descending());

        Integer type = request.getCriteriaType();
        String value = request.getCriteriaValue();

        Page<ReturnListResponse> resultPage;

        if (type != null && type == 1 && value != null && !value.isBlank()) {
            ReturnListResponse detail = returnRequestRepository.findReturnListByReturnId(value)
                    .orElseThrow(() -> new ReturnNotFoundException(value));
            return ResponseErrorTemplate.success("Return retrieved successfully", List.of(detail));
        }

        if (type == null || value == null || value.isBlank()) {
            resultPage = returnRequestRepository.findAllReturns(pageable);
        } else if (type == 2) {
            resultPage = returnRequestRepository.findByOrderNumber(value, pageable);
        } else if (type == 3) {
            resultPage = returnRequestRepository.findByCustomerNameContaining(value, pageable);
        } else if (type == 4) {
            resultPage = returnRequestRepository.findByProductNameContaining(value, pageable);
        } else if (type == 5) {
            resultPage = returnRequestRepository.findByStatus(value, pageable);
        } else if (type == 6) {
            resultPage = returnRequestRepository.findByReturnType(value, pageable);
        } else {
            resultPage = returnRequestRepository.findAllReturns(pageable);
        }

        ReturnPageResponse pageResponse = ReturnPageResponse.builder()
                .payload(resultPage.getContent())
                .totalItems(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .currentPage(resultPage.getNumber() + 1)
                .pageSize(resultPage.getSize())
                .build();

        String message = resultPage.isEmpty() ? "No returns found" : "Returns retrieved successfully";
        return ResponseErrorTemplate.success(message, pageResponse);
    }
}
