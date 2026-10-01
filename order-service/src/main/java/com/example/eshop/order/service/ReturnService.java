package com.example.eshop.order.service;

import com.example.eshop.order.dto.request.*;
import com.example.eshop.order.dto.response.ResponseErrorTemplate;

public interface ReturnService {

    ResponseErrorTemplate createReturn(CreateReturnRequest request);

    ResponseErrorTemplate getReturnSummary();

    ResponseErrorTemplate getReturnDetail(String returnId);

    ResponseErrorTemplate getReturnHistory(String returnId);

    ResponseErrorTemplate approveReturn(String returnId, ApproveReturnRequest request);

    ResponseErrorTemplate rejectReturn(String returnId, RejectReturnRequest request);

    ResponseErrorTemplate receiveReturn(String returnId, ReceiveReturnRequest request);

    ResponseErrorTemplate startInspection(String returnId);

    ResponseErrorTemplate completeInspection(String returnId, CompleteInspectionRequest request);

    ResponseErrorTemplate getReturns(GetReturnRequest request);
}
