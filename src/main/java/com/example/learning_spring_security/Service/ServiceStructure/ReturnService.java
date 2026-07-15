package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.ApproveReturnRequest;
import com.example.learning_spring_security.dto.Request.CompleteInspectionRequest;
import com.example.learning_spring_security.dto.Request.CreateReturnRequest;
import com.example.learning_spring_security.dto.Request.GetReturnListRequest;
import com.example.learning_spring_security.dto.Request.ReceiveReturnRequest;
import com.example.learning_spring_security.dto.Request.RejectReturnRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;

public interface ReturnService {

    ResponseErrorTemplate createReturn(CreateReturnRequest request);

    ResponseErrorTemplate getReturnSummary();

    ResponseErrorTemplate getReturnList(GetReturnListRequest request);

    ResponseErrorTemplate getReturnDetail(String returnId);

    ResponseErrorTemplate getReturnHistory(String returnId);

    ResponseErrorTemplate approveReturn(String returnId, ApproveReturnRequest request);

    ResponseErrorTemplate rejectReturn(String returnId, RejectReturnRequest request);

    ResponseErrorTemplate receiveReturn(String returnId, ReceiveReturnRequest request);

    ResponseErrorTemplate startInspection(String returnId);

    ResponseErrorTemplate completeInspection(String returnId, CompleteInspectionRequest request);
}