package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.*;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;

public interface ReturnService {

    ResponseErrorTemplate createReturn(CreateReturnRequest request);

    ResponseErrorTemplate getReturnSummary();

//    ResponseErrorTemplate getReturnList(GetReturnListRequest request);

    ResponseErrorTemplate getReturnDetail(String returnId);

    ResponseErrorTemplate getReturnHistory(String returnId);

    ResponseErrorTemplate approveReturn(String returnId, ApproveReturnRequest request);

    ResponseErrorTemplate rejectReturn(String returnId, RejectReturnRequest request);

    ResponseErrorTemplate receiveReturn(String returnId, ReceiveReturnRequest request);

    ResponseErrorTemplate startInspection(String returnId);

    ResponseErrorTemplate completeInspection(String returnId, CompleteInspectionRequest request);

    ResponseErrorTemplate getReturns(GetReturnRequest request);
}