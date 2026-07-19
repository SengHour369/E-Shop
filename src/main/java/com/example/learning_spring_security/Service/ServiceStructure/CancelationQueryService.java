package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.GetCancelationListRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.cache.annotation.Cacheable;

public interface CancelationQueryService {

    @Cacheable(value = "cancelations", key = "'summary'")
    ResponseErrorTemplate getCancelationSummary();

    // Paginated – skip
    ResponseErrorTemplate getCancelationList(GetCancelationListRequest request);

    @Cacheable(value = "cancelations", key = "#orderNo")
    ResponseErrorTemplate getCancelationDetail(String orderNo);
}