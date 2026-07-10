package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.BadRequestException;
import com.example.learning_spring_security.Exception.ExceptionService.CancelationNotFoundException;
import com.example.learning_spring_security.Repository.OrderCancelationRepository;
import com.example.learning_spring_security.Repository.OrderRepository;
import com.example.learning_spring_security.Service.ServiceStructure.CancelationQueryService;
import com.example.learning_spring_security.dto.Request.GetCancelationListRequest;
import com.example.learning_spring_security.dto.Response.CancelationDetailResponse;
import com.example.learning_spring_security.dto.Response.CancelationListResponse;
import com.example.learning_spring_security.dto.Response.CancelationPageResponse;
import com.example.learning_spring_security.dto.Response.CancelationSummaryResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CancelationQueryServiceImpl implements CancelationQueryService {

    private final OrderCancelationRepository orderCancelationRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getCancelationSummary() {
        CancelationSummaryResponse summary = orderCancelationRepository.getCancelationSummary();

        long totalOrders = orderRepository.count();
        long totalCancelations = summary.getTotalCancelations() != null ? summary.getTotalCancelations() : 0L;
        double cancelationRate = totalOrders == 0 ? 0.0 : Math.round(totalCancelations * 1000.0 / totalOrders) / 10.0;
        summary.setCancelationRate(cancelationRate);

        return ResponseErrorTemplate.success("Cancelation summary retrieved successfully", summary);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getCancelationList(GetCancelationListRequest request) {
        if (request.getFromCancelDate() != null && request.getToCancelDate() != null
                && request.getFromCancelDate().isAfter(request.getToCancelDate())) {
            throw new BadRequestException("fromCancelDate must not be greater than toCancelDate");
        }
        if (request.getMinAmount() != null && request.getMaxAmount() != null
                && request.getMinAmount().compareTo(request.getMaxAmount()) > 0) {
            throw new BadRequestException("minAmount must not be greater than maxAmount");
        }

        int page = request.getPage() == null || request.getPage() < 1 ? 1 : request.getPage();
        int size = request.getSize() == null || request.getSize() < 1 ? 10 : request.getSize();
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("cancelDate").descending());

        Page<CancelationListResponse> result = orderCancelationRepository.search(
                request.getOrderNo(),
                request.getCustomerName(),
                request.getCancelReason(),
                request.getCancelStatus(),
                request.getFromCancelDate(),
                request.getToCancelDate(),
                request.getMinAmount(),
                request.getMaxAmount(),
                pageable
        );

        CancelationPageResponse pageResponse = CancelationPageResponse.builder()
                .payload(result.getContent())
                .totalItems(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .currentPage(result.getNumber() + 1)
                .pageSize(result.getSize())
                .build();

        String message = result.isEmpty() ? "No cancelations found" : "Cancelations retrieved successfully";
        return ResponseErrorTemplate.success(message, pageResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getCancelationDetail(String orderNo) {
        CancelationDetailResponse detail = orderCancelationRepository.findDetailByOrderNo(orderNo)
                .orElseThrow(() -> new CancelationNotFoundException(orderNo));
        return ResponseErrorTemplate.success("Cancelation detail retrieved successfully", detail);
    }
}