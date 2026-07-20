package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.GetCancelationListRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;

public interface CancelationQueryService {

    ResponseErrorTemplate getCancelationSummary();

    ResponseErrorTemplate getCancelationList(GetCancelationListRequest request);

    ResponseErrorTemplate getCancelationDetail(String orderNo);
}