package com.example.eshop.order.service;

import com.example.eshop.order.model.Return;
import com.example.eshop.order.dto.request.CancelRefundRequest;
import com.example.eshop.order.dto.request.GetRefundListRequest;
import com.example.eshop.order.dto.request.ProcessRefundRequest;
import com.example.eshop.order.dto.response.ResponseErrorTemplate;

public interface RefundService {

    ResponseErrorTemplate getRefundSummary();

    ResponseErrorTemplate getRefundList(GetRefundListRequest request);

    ResponseErrorTemplate getRefundDetail(String refundId);

    ResponseErrorTemplate getRefundHistory(String refundId);

    ResponseErrorTemplate processRefund(String refundId, ProcessRefundRequest request);

    ResponseErrorTemplate cancelRefund(String refundId, CancelRefundRequest request);

    /**
     * Returns products similar to the ones in the refunded order.
     * TODO: cross-service call via Feign to catalog-service — this used to delegate to a local
     * ProductService which no longer lives in this service.
     */
    ResponseErrorTemplate getSimilarProducts(String refundId, Integer page, Integer size);

    /**
     * Creates a PENDING refund for an approved return (BR-001/002/003/004).
     * Idempotent: does nothing if a refund already exists for this return.
     */
    void createRefundFromReturn(Return returnRequest);
}
