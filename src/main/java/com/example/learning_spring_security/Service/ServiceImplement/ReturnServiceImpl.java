package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Constant.ReturnStatus;
import com.example.learning_spring_security.Constant.ReturnType;
import com.example.learning_spring_security.Exception.ExceptionService.BadRequestException;
import com.example.learning_spring_security.Exception.ExceptionService.InvalidReturnStatusException;
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
import com.example.learning_spring_security.dto.Request.ApproveReturnRequest;
import com.example.learning_spring_security.dto.Request.CompleteInspectionRequest;
import com.example.learning_spring_security.dto.Request.GetReturnListRequest;
import com.example.learning_spring_security.dto.Request.ReceiveReturnRequest;
import com.example.learning_spring_security.dto.Request.RejectReturnRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import com.example.learning_spring_security.dto.Response.ReturnListResponse;
import com.example.learning_spring_security.dto.Response.ReturnPageResponse;
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

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnServiceImpl implements ReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final ReturnStatusHistoryRepository returnStatusHistoryRepository;
    private final RefundService refundService;
    private final InventoryRepository inventoryRepository;

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
    public ResponseErrorTemplate getReturnList(GetReturnListRequest request) {
        if (request.getFromDate() != null && request.getToDate() != null
                && request.getFromDate().isAfter(request.getToDate())) {
            throw new BadRequestException("fromDate must not be greater than toDate");
        }

        int page = request.getPage() == null || request.getPage() < 1 ? 1 : request.getPage();
        int size = request.getSize() == null || request.getSize() < 1 ? 10 : request.getSize();
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("requestedAt").descending());

        Page<ReturnListResponse> result = returnRequestRepository.search(
                request.getReturnId(),
                request.getOrderNo(),
                request.getCustomerName(),
                request.getProductName(),
                request.getReturnType(),
                request.getStatus(),
                request.getFromDate(),
                request.getToDate(),
                pageable
        );

        ReturnPageResponse pageResponse = ReturnPageResponse.builder()
                .payload(result.getContent())
                .totalItems(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .currentPage(result.getNumber() + 1)
                .pageSize(result.getSize())
                .build();

        String message = result.isEmpty() ? "No returns found" : "Returns retrieved successfully";
        return ResponseErrorTemplate.success(message, pageResponse);
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
}