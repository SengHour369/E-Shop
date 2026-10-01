package com.example.eshop.order.service;

import com.example.eshop.order.dto.request.GetCancelationListRequest;
import com.example.eshop.order.dto.response.ResponseErrorTemplate;

public interface CancelationQueryService {

    ResponseErrorTemplate getCancelationSummary();

    ResponseErrorTemplate getCancelationList(GetCancelationListRequest request);

    ResponseErrorTemplate getCancelationDetail(String orderNo);
}
