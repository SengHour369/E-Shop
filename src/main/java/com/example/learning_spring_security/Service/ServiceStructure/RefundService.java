package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.Model.Return;
import com.example.learning_spring_security.dto.Request.CancelRefundRequest;
import com.example.learning_spring_security.dto.Request.GetRefundListRequest;
import com.example.learning_spring_security.dto.Request.ProcessRefundRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;

public interface RefundService {

    ResponseErrorTemplate getRefundSummary();

    ResponseErrorTemplate getRefundList(GetRefundListRequest request);

    ResponseErrorTemplate getRefundDetail(String refundId);

    ResponseErrorTemplate getRefundHistory(String refundId);

    ResponseErrorTemplate processRefund(String refundId, ProcessRefundRequest request);

    ResponseErrorTemplate cancelRefund(String refundId, CancelRefundRequest request);

    /**
     * Returns products similar to the ones in the refunded order (same sub-category),
     * delegating to {@code ProductService.getProducts} for the actual lookup/pagination.
     */
    ResponseErrorTemplate getSimilarProducts(String refundId, Integer page, Integer size);

    /**
     * Creates a PENDING refund for an approved return (BR-001/002/003/004).
     * Idempotent: does nothing if a refund already exists for this return.
     */
    void createRefundFromReturn(Return returnRequest);
}