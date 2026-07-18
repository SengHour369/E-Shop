package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Constant.ReturnStatus;
import com.example.learning_spring_security.Constant.ReturnType;
import com.example.learning_spring_security.Exception.ExceptionService.BadRequestException;
import com.example.learning_spring_security.Exception.ExceptionService.InvalidReturnStatusException;
import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Exception.ExceptionService.ReturnNotFoundException;
import com.example.learning_spring_security.Model.OrderItem;
import com.example.learning_spring_security.Model.ProductSku;
import com.example.learning_spring_security.Model.Return;
import com.example.learning_spring_security.Model.ReturnStatusHistory;
import com.example.learning_spring_security.Repository.InventoryRepository;
import com.example.learning_spring_security.Repository.OrderRepository;
import com.example.learning_spring_security.Repository.ReturnRequestRepository;
import com.example.learning_spring_security.Repository.ReturnStatusHistoryRepository;
import com.example.learning_spring_security.Service.ServiceStructure.RefundService;
import com.example.learning_spring_security.Service.ServiceStructure.ReturnService;
import com.example.learning_spring_security.dto.Request.*;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import com.example.learning_spring_security.dto.Response.ReturnListResponse;
import com.example.learning_spring_security.dto.Response.ReturnSummaryResponse;
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
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public ResponseErrorTemplate createReturn(CreateReturnRequest request) {
        orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.getOrderId()));

        String returnType = request.getReturnType() == null ? null : request.getReturnType().trim().toUpperCase();
        if (!VALID_RETURN_TYPES.contains(returnType)) {
            throw new BadRequestException("Invalid return type: " + request.getReturnType()
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
            increaseInventoryForReturnedProduct(returnRequest);

            if (!ReturnType.EXCHANGE.equals(returnRequest.getReturnType())) {
                refundService.createRefundFromReturn(returnRequest);
            } else {
                log.warn("Return {} is an EXCHANGE — replacement order/shipment creation is not yet implemented", returnId);
            }
        }

        return getReturnDetail(returnId);
    }

    private void increaseInventoryForReturnedProduct(Return returnRequest) {
        Long returnedQuantity = orderRepository.findByIdWithItems(returnRequest.getOrderId())
                .flatMap(order -> order.getOrderItems().stream()
                        .filter(item -> item.getProductSku().getProduct().getId().equals(returnRequest.getProductId()))
                        .map(OrderItem::getQuantity)
                        .findFirst())
                .orElseGet(() -> {
                    log.warn("Could not find matching order item for orderId={}, productId={} on return {}; defaulting restocked quantity to 1",
                            returnRequest.getOrderId(), returnRequest.getProductId(), returnRequest.getReturnId());
                    return 1L;
                });

        inventoryRepository.findDefaultSkuByProductId(returnRequest.getProductId())
                .map(ProductSku::getId)
                .ifPresentOrElse(
                        skuId -> inventoryRepository.increaseStock(skuId, returnedQuantity),
                        () -> log.warn("No default SKU found for productId={}, skipping inventory update for return {}",
                                returnRequest.getProductId(), returnRequest.getReturnId())
                );
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

        // ── Single‑item lookup (no pagination needed) ──────────────
        if (type != null && type == 1 && value != null && !value.isBlank()) {
            // by returnId – return a single item wrapped as a page
            ReturnListResponse detail = returnRequestRepository.findReturnListByReturnId(value)
                    .orElseThrow(() -> new ReturnNotFoundException(value));
            return ResponseErrorTemplate.success("Return retrieved successfully",
                    List.of(detail));   // or wrap in a page if you prefer
        }

        // ── Paginated lookups ────────────────────────────────────────
        if (type == null || value == null || value.isBlank()) {
            // no filter → all returns
            resultPage = returnRequestRepository.findAllReturns(pageable);
        } else if (type == 2) {
            // by order number
            resultPage = returnRequestRepository.findByOrderNumber(value, pageable);
        } else if (type == 3) {
            // by customer name (fuzzy)
            resultPage = returnRequestRepository.findByCustomerNameContaining(value, pageable);
        } else if (type == 4) {
            // by product name (fuzzy)
            resultPage = returnRequestRepository.findByProductNameContaining(value, pageable);
        } else if (type == 5) {
            // by status (exact)
            resultPage = returnRequestRepository.findByStatus(value, pageable);
        } else if (type == 6) {
            // by return type (exact)
            resultPage = returnRequestRepository.findByReturnType(value, pageable);
        } else {
            // fallback: all
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