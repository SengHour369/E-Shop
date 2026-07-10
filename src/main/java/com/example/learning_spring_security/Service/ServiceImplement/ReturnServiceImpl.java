package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Constant.ReturnStatus;
import com.example.learning_spring_security.Exception.ExceptionService.BadRequestException;
import com.example.learning_spring_security.Exception.ExceptionService.InvalidReturnStatusException;
import com.example.learning_spring_security.Exception.ExceptionService.ReturnNotFoundException;
import com.example.learning_spring_security.Model.Return;
import com.example.learning_spring_security.Repository.OrderRepository;
import com.example.learning_spring_security.Repository.ReturnRequestRepository;
import com.example.learning_spring_security.Service.ServiceStructure.ReturnService;
import com.example.learning_spring_security.dto.Request.ApproveReturnRequest;
import com.example.learning_spring_security.dto.Request.GetReturnListRequest;
import com.example.learning_spring_security.dto.Request.RejectReturnRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import com.example.learning_spring_security.dto.Response.ReturnListResponse;
import com.example.learning_spring_security.dto.Response.ReturnPageResponse;
import com.example.learning_spring_security.dto.Response.ReturnSummaryResponse;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnServiceImpl implements ReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;

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
    @Transactional
    public ResponseErrorTemplate approveReturn(String returnId, ApproveReturnRequest request) {
        Return returnRequest = returnRequestRepository.findByReturnId(returnId)
                .orElseThrow(() -> new ReturnNotFoundException(returnId));

        if (!ReturnStatus.REQUESTED.equals(returnRequest.getStatus())) {
            throw new InvalidReturnStatusException(returnId, returnRequest.getStatus());
        }

        String actor = currentUsername();
        returnRequest.setStatus(ReturnStatus.APPROVED);
        returnRequest.setApprovedAt(LocalDateTime.now());
        returnRequest.setApprovedBy(actor);
        returnRequest.setUpdatedBy(actor);
        if (request != null && request.getRemark() != null) {
            returnRequest.setRemark(request.getRemark());
        }

        returnRequestRepository.save(returnRequest);
        log.info("Return {} approved by {}", returnId, actor);

        return getReturnDetail(returnId);
    }

    @Override
    @Transactional
    public ResponseErrorTemplate rejectReturn(String returnId, RejectReturnRequest request) {
        Return returnRequest = returnRequestRepository.findByReturnId(returnId)
                .orElseThrow(() -> new ReturnNotFoundException(returnId));

        if (!ReturnStatus.REQUESTED.equals(returnRequest.getStatus())) {
            throw new InvalidReturnStatusException(returnId, returnRequest.getStatus());
        }

        String actor = currentUsername();
        returnRequest.setStatus(ReturnStatus.REJECTED);
        returnRequest.setRejectedAt(LocalDateTime.now());
        returnRequest.setRejectedBy(actor);
        returnRequest.setUpdatedBy(actor);
        if (request != null && request.getRemark() != null) {
            returnRequest.setRemark(request.getRemark());
        }

        returnRequestRepository.save(returnRequest);
        log.info("Return {} rejected by {}", returnId, actor);

        return getReturnDetail(returnId);
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }
}