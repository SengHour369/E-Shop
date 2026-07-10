package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.ApproveReturnRequest;
import com.example.learning_spring_security.dto.Request.GetReturnListRequest;
import com.example.learning_spring_security.dto.Request.RejectReturnRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;

public interface ReturnService {

    ResponseErrorTemplate getReturnSummary();

    ResponseErrorTemplate getReturnList(GetReturnListRequest request);

    ResponseErrorTemplate getReturnDetail(String returnId);

    ResponseErrorTemplate approveReturn(String returnId, ApproveReturnRequest request);

    ResponseErrorTemplate rejectReturn(String returnId, RejectReturnRequest request);
}