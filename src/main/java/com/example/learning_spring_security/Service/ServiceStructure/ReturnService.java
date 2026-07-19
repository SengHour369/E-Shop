package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.*;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

public interface ReturnService {

    @CacheEvict(value = "returns", allEntries = true)
    ResponseErrorTemplate createReturn(CreateReturnRequest request);

    @Cacheable(value = "returns", key = "'summary'")
    ResponseErrorTemplate getReturnSummary();

    // Paginated – skip
    ResponseErrorTemplate getReturns(GetReturnRequest request);

    @Cacheable(value = "returns", key = "#returnId")
    ResponseErrorTemplate getReturnDetail(String returnId);

    @Cacheable(value = "returns", key = "#returnId + ':history'")
    ResponseErrorTemplate getReturnHistory(String returnId);

    @CacheEvict(value = "returns", key = "#returnId")
    ResponseErrorTemplate approveReturn(String returnId, ApproveReturnRequest request);

    @CacheEvict(value = "returns", key = "#returnId")
    ResponseErrorTemplate rejectReturn(String returnId, RejectReturnRequest request);

    @CacheEvict(value = "returns", key = "#returnId")
    ResponseErrorTemplate receiveReturn(String returnId, ReceiveReturnRequest request);

    @CacheEvict(value = "returns", key = "#returnId")
    ResponseErrorTemplate startInspection(String returnId);

    @CacheEvict(value = "returns", key = "#returnId")
    ResponseErrorTemplate completeInspection(String returnId, CompleteInspectionRequest request);
}